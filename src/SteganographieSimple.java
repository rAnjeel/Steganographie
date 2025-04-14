import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Implémentation simplifiée de stéganographie utilisant Huffman et LSB
 */
public class SteganographieSimple {
    
    /**
     * Cache un message dans une image PNG en utilisant le codage Huffman et LSB
     */
    public static void cacher(String cheminImage, String cheminSortie, String message) throws IOException {
        // 1. Préparer le message avec codage Huffman
        byte[] messageBytes = preparerMessage(message);
        
        // 2. Charger l'image
        BufferedImage image = ImageIO.read(new File(cheminImage));
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        System.out.println("Dimensions de l'image: " + largeur + "x" + hauteur);
        
        // 3. Vérifier si l'image est assez grande
        if (messageBytes.length * 8 > largeur * hauteur) {
            throw new IOException("Image trop petite pour le message (" + messageBytes.length + " octets)");
        }
        
        // 4. Cacher les octets du message dans les LSB des pixels
        int indexOctet = 0;
        int bitPosition = 0;
        
        // D'abord, cacher la taille du message (4 octets = int)
        int taille = messageBytes.length;
        for (int i = 0; i < 32; i++) {
            int x = i % largeur;
            int y = i / largeur;
            int bit = (taille >> i) & 1;
            
            int pixel = image.getRGB(x, y);
            // Modifier le LSB du canal bleu
            pixel = (pixel & 0xFFFFFFFE) | bit;
            image.setRGB(x, y, pixel);
        }
        
        // Ensuite, cacher les octets du message
        for (int i = 0; i < messageBytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                int index = 32 + (i * 8) + j; // Commencer après la taille
                int x = index % largeur;
                int y = index / largeur;
                
                int bit = (messageBytes[i] >> j) & 1;
                
                int pixel = image.getRGB(x, y);
                // Modifier le LSB du pixel
                pixel = (pixel & 0xFFFFFFFE) | bit;
                image.setRGB(x, y, pixel);
            }
        }
        
        // 5. Sauvegarder l'image résultante
        ImageIO.write(image, "png", new File(cheminSortie));
        System.out.println("Message de " + messageBytes.length + " octets caché avec succès");
    }
    
    /**
     * Extrait un message caché dans une image PNG
     */
    public static String extraire(String cheminImage) throws IOException {
        // 1. Charger l'image
        BufferedImage image = ImageIO.read(new File(cheminImage));
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        System.out.println("Dimensions de l'image: " + largeur + "x" + hauteur);
        
        // 2. D'abord, récupérer la taille du message (4 octets = int)
        int taille = 0;
        for (int i = 0; i < 32; i++) {
            int x = i % largeur;
            int y = i / largeur;
            
            int pixel = image.getRGB(x, y);
            int bit = pixel & 1;
            
            taille |= (bit << i);
        }
        
        System.out.println("Taille du message à extraire: " + taille + " octets");
        
        // Vérifier si la taille est plausible
        if (taille <= 0 || taille > (largeur * hauteur / 8)) {
            System.out.println("Taille de message invraisemblable, utilisation d'une valeur par défaut");
            taille = 100; // Taille par défaut
        }
        
        // 3. Récupérer les octets du message
        byte[] messageBytes = new byte[taille];
        for (int i = 0; i < taille; i++) {
            for (int j = 0; j < 8; j++) {
                int index = 32 + (i * 8) + j; // Commencer après la taille
                int x = index % largeur;
                int y = index / largeur;
                
                int pixel = image.getRGB(x, y);
                int bit = pixel & 1;
                
                messageBytes[i] |= (bit << j);
            }
        }
        
        // 4. Décoder le message
        return decoderMessage(messageBytes);
    }
    
    /**
     * Prépare le message en utilisant Huffman et sérialisation simple
     */
    private static byte[] preparerMessage(String message) {
        try {
            // 1. Calculer les fréquences des caractères
            Map<Character, Integer> frequences = new HashMap<>();
            for (char c : message.toCharArray()) {
                frequences.put(c, frequences.getOrDefault(c, 0) + 1);
            }
            
            // 2. Construire l'arbre de Huffman
            NoeudHuffman racine = CodageHuffman.construireArbreHuffman(frequences);
            CodageHuffman.genererCodes(racine, "");
            
            // 3. Encoder le message
            String messageBinaire = CodageHuffman.encoder(message);
            
            // 4. Préparer la sérialisation
            StringBuilder serialisation = new StringBuilder();
            
            // Format:
            // - Taille du dictionnaire (8 bits)
            // - Pour chaque caractère:
            //   - Caractère (16 bits)
            //   - Fréquence (16 bits)
            // - Taille du message encodé (16 bits)
            // - Message encodé (bits variables)
            
            // Taille du dictionnaire (8 bits)
            serialisation.append(String.format("%8s", Integer.toBinaryString(frequences.size())).replace(' ', '0'));
            
            // Dictionnaire
            for (Map.Entry<Character, Integer> entry : frequences.entrySet()) {
                // Caractère (16 bits)
                serialisation.append(String.format("%16s", Integer.toBinaryString(entry.getKey())).replace(' ', '0'));
                
                // Fréquence (16 bits)
                serialisation.append(String.format("%16s", Integer.toBinaryString(entry.getValue())).replace(' ', '0'));
            }
            
            // Taille du message encodé (16 bits)
            serialisation.append(String.format("%16s", Integer.toBinaryString(messageBinaire.length())).replace(' ', '0'));
            
            // Message encodé
            serialisation.append(messageBinaire);
            
            // 5. Convertir en octets
            String binaire = serialisation.toString();
            int nbOctets = (binaire.length() + 7) / 8;
            byte[] resultat = new byte[nbOctets];
            
            for (int i = 0; i < binaire.length(); i++) {
                if (binaire.charAt(i) == '1') {
                    resultat[i / 8] |= (1 << (i % 8));
                }
            }
            
            return resultat;
        } catch (Exception e) {
            // En cas d'erreur, utiliser l'encodage ASCII simple
            System.out.println("Erreur lors de la préparation du message: " + e.getMessage());
            byte[] resultat = new byte[message.length()];
            for (int i = 0; i < message.length(); i++) {
                resultat[i] = (byte) message.charAt(i);
            }
            return resultat;
        }
    }
    
    /**
     * Décode le message à partir des octets
     */
    private static String decoderMessage(byte[] messageBytes) {
        try {
            // 1. Convertir en chaîne binaire
            StringBuilder binaire = new StringBuilder();
            for (int i = 0; i < messageBytes.length * 8; i++) {
                binaire.append((messageBytes[i / 8] >> (i % 8)) & 1);
            }
            
            // 2. Extraire les informations
            String bin = binaire.toString();
            
            // Taille du dictionnaire (8 bits)
            int position = 0;
            int tailleDictionnaire = Integer.parseInt(bin.substring(position, position + 8), 2);
            position += 8;
            
            System.out.println("Taille du dictionnaire: " + tailleDictionnaire);
            
            // Dictionnaire
            Map<Character, Integer> frequences = new HashMap<>();
            for (int i = 0; i < tailleDictionnaire && position + 32 <= bin.length(); i++) {
                // Caractère (16 bits)
                char caractere = (char) Integer.parseInt(bin.substring(position, position + 16), 2);
                position += 16;
                
                // Fréquence (16 bits)
                int frequence = Integer.parseInt(bin.substring(position, position + 16), 2);
                position += 16;
                
                frequences.put(caractere, frequence);
            }
            
            System.out.println("Dictionnaire extrait: " + frequences);
            
            // Si le dictionnaire est vide ou invalide, essayer l'ASCII
            if (frequences.isEmpty() || position + 16 > bin.length()) {
                return extraireASCII(messageBytes);
            }
            
            // Taille du message encodé (16 bits)
            int tailleMessage = Integer.parseInt(bin.substring(position, position + 16), 2);
            position += 16;
            
            System.out.println("Taille du message encodé: " + tailleMessage + " bits");
            
            // Si taille invalide, essayer l'ASCII
            if (tailleMessage <= 0 || position + tailleMessage > bin.length()) {
                return extraireASCII(messageBytes);
            }
            
            // Message encodé
            String messageBinaire = bin.substring(position, position + tailleMessage);
            
            // 3. Reconstruire l'arbre de Huffman
            NoeudHuffman racine = CodageHuffman.construireArbreHuffman(frequences);
            CodageHuffman.genererCodes(racine, "");
            
            // 4. Décoder le message
            String message = CodageHuffman.decoder(messageBinaire);
            
            // Si le décodage échoue, essayer l'ASCII
            if (message == null || message.isEmpty()) {
                return extraireASCII(messageBytes);
            }
            
            return message;
        } catch (Exception e) {
            System.out.println("Erreur lors du décodage du message: " + e.getMessage());
            return extraireASCII(messageBytes);
        }
    }
    
    /**
     * Extraction directe en ASCII en cas d'échec
     */
    private static String extraireASCII(byte[] messageBytes) {
        StringBuilder message = new StringBuilder();
        for (byte b : messageBytes) {
            if (b >= 32 && b <= 126) { // Caractères ASCII imprimables
                message.append((char) b);
            }
        }
        return message.toString();
    }
} 