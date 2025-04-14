import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import javax.imageio.ImageIO;

public class SteganographiePNG {
    
    // Cache le message dans l'image
    public static void cacherMessage(String cheminImage, String cheminSortie, String message, String prenom, int numero) throws IOException {
        // Convertir le message en chaîne binaire à l'aide du codage Huffman
        Map<Character, Integer> frequences = new HashMap<>();
        for (char c : message.toCharArray()) {
            frequences.put(c, frequences.getOrDefault(c, 0) + 1);
        }
        
        NoeudHuffman racineHuffman = CodageHuffman.construireArbreHuffman(frequences);
        CodageHuffman.genererCodes(racineHuffman, "");
        String messageBinaire = CodageHuffman.encoder(message);
        
        // Ajouter la longueur du message binaire et la table de fréquences au début
        String donneesCompletes = serializerDonnees(messageBinaire, frequences);
        
        // Lire l'image
        BufferedImage image = ImageIO.read(new File(cheminImage));
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        
        // Générer un générateur de nombres aléatoires basé sur le prénom et le numéro
        Random random = initialiserRandom(prenom, numero);
        
        // Cacher les données dans l'image
        int indexBit = 0;
        int totalBits = donneesCompletes.length();
        
        // Vérifier si l'image est assez grande pour contenir le message
        if (totalBits > largeur * hauteur * 3) {
            throw new IOException("L'image est trop petite pour contenir le message.");
        }
        
        // Utiliser un tableau pour suivre les positions déjà utilisées
        boolean[] positionsUtilisees = new boolean[largeur * hauteur];
        
        while (indexBit < totalBits) {
            // Générer une position aléatoire
            int x = random.nextInt(largeur);
            int y = random.nextInt(hauteur);
            int position = y * largeur + x;
            
            // Si cette position a déjà été utilisée, en trouver une autre
            if (positionsUtilisees[position]) {
                continue;
            }
            positionsUtilisees[position] = true;
            
            // Obtenir la couleur RGB du pixel
            int pixel = image.getRGB(x, y);
            
            // Modifier le bit le moins significatif d'une des composantes RGB
            int composante = random.nextInt(3); // 0: Rouge, 1: Vert, 2: Bleu
            
            int rouge = (pixel >> 16) & 0xff;
            int vert = (pixel >> 8) & 0xff;
            int bleu = pixel & 0xff;
            int alpha = (pixel >> 24) & 0xff;
            
            // Remplacer le LSB de la composante sélectionnée
            char bit = donneesCompletes.charAt(indexBit);
            int valeurBit = (bit == '1') ? 1 : 0;
            
            switch (composante) {
                case 0: // Rouge
                    rouge = (rouge & 0xFE) | valeurBit; // 0xFE = 11111110 binaire (met le dernier bit à 0)
                    break;
                case 1: // Vert
                    vert = (vert & 0xFE) | valeurBit;
                    break;
                case 2: // Bleu
                    bleu = (bleu & 0xFE) | valeurBit;
                    break;
            }
            
            // Reconstituer le pixel
            int nouveauPixel = (alpha << 24) | (rouge << 16) | (vert << 8) | bleu;
            image.setRGB(x, y, nouveauPixel);
            
            indexBit++;
        }
        
        // Enregistrer l'image modifiée
        ImageIO.write(image, "png", new File(cheminSortie));
        
        System.out.println("Message caché avec succès. Bits utilisés: " + totalBits + " sur " + (largeur * hauteur * 3) + " disponibles.");
    }
    
    // Extrait le message caché de l'image
    public static String extraireMessage(String cheminImage, String prenom, int numero) throws IOException, ClassNotFoundException {
        // Lire l'image
        BufferedImage image = ImageIO.read(new File(cheminImage));
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        
        // Générer un générateur de nombres aléatoires basé sur le prénom et le numéro
        Random random = initialiserRandom(prenom, numero);
        
        // Générer les mêmes positions aléatoires que lors du masquage pour extraire les 32 premiers bits (longueur totale)
        StringBuilder longueurBinaire = new StringBuilder();
        boolean[] positionsUtilisees = new boolean[largeur * hauteur];
        
        // Extraire les 32 premiers bits qui représentent la longueur totale
        for (int i = 0; i < 32; i++) {
            // Générer une position aléatoire
            int x = random.nextInt(largeur);
            int y = random.nextInt(hauteur);
            int position = y * largeur + x;
            
            // Si cette position a déjà été utilisée, en trouver une autre
            while (positionsUtilisees[position]) {
                x = random.nextInt(largeur);
                y = random.nextInt(hauteur);
                position = y * largeur + x;
            }
            positionsUtilisees[position] = true;
            
            int pixel = image.getRGB(x, y);
            int composante = random.nextInt(3);
            
            int valeur = 0;
            switch (composante) {
                case 0: // Rouge
                    valeur = (pixel >> 16) & 1;
                    break;
                case 1: // Vert
                    valeur = (pixel >> 8) & 1;
                    break;
                case 2: // Bleu
                    valeur = pixel & 1;
                    break;
            }
            
            longueurBinaire.append(valeur);
        }
        
        try {
            // Convertir la chaîne binaire en un entier - avec une valeur maximale pour éviter les erreurs
            int longueurTotale = Integer.parseInt(longueurBinaire.toString(), 2);
            
            // Limiter la longueur pour éviter les dépassements
            longueurTotale = Math.min(longueurTotale, largeur * hauteur);
            
            // Si la longueur totale est absurde, c'est probablement une erreur
            if (longueurTotale <= 0 || longueurTotale > largeur * hauteur * 3 / 2) {
                throw new IllegalArgumentException("Longueur de données invalide: " + longueurTotale);
            }
            
            System.out.println("Longueur des données à extraire: " + longueurTotale + " bits");
            
            // Extraire les données complètes
            StringBuilder donneesCompletes = new StringBuilder(longueurBinaire.toString());
            
            // Réinitialiser le générateur aléatoire et les positions utilisées pour extraire le reste des données
            random = initialiserRandom(prenom, numero);
            positionsUtilisees = new boolean[largeur * hauteur];
            
            // Refaire le même parcours en commençant par extraire les 32 premiers bits (longueur)
            for (int i = 0; i < 32; i++) {
                int x = random.nextInt(largeur);
                int y = random.nextInt(hauteur);
                int position = y * largeur + x;
                
                while (positionsUtilisees[position]) {
                    x = random.nextInt(largeur);
                    y = random.nextInt(hauteur);
                    position = y * largeur + x;
                }
                positionsUtilisees[position] = true;
                
                random.nextInt(3); // Composante utilisée
            }
            
            // Puis extraire le reste des données
            for (int i = 32; i < longueurTotale; i++) {
                // Générer une position aléatoire
                int x = random.nextInt(largeur);
                int y = random.nextInt(hauteur);
                int position = y * largeur + x;
                
                // Si cette position a déjà été utilisée, en trouver une autre
                while (positionsUtilisees[position]) {
                    x = random.nextInt(largeur);
                    y = random.nextInt(hauteur);
                    position = y * largeur + x;
                }
                positionsUtilisees[position] = true;
                
                int pixel = image.getRGB(x, y);
                int composante = random.nextInt(3);
                
                int valeur = 0;
                switch (composante) {
                    case 0: // Rouge
                        valeur = (pixel >> 16) & 1;
                        break;
                    case 1: // Vert
                        valeur = (pixel >> 8) & 1;
                        break;
                    case 2: // Bleu
                        valeur = pixel & 1;
                        break;
                }
                
                donneesCompletes.append(valeur);
            }
            
            // Désérialiser les données
            return deserializerDonnees(donneesCompletes.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La stéganographie a échoué: impossible de lire la longueur des données: " + e.getMessage());
        }
    }
    
    // Initialiser le générateur de nombres aléatoires en fonction du prénom et du numéro
    private static Random initialiserRandom(String prenom, int numero) {
        long seed = 0;
        for (char c : prenom.toCharArray()) {
            seed = 31 * seed + c;
        }
        seed = seed * 31 + numero;
        return new Random(seed);
    }
    
    // Sérialise les données (messageBinaire + fréquences) en chaîne binaire
    private static String serializerDonnees(String messageBinaire, Map<Character, Integer> frequences) {
        // À implémenter : sérialisation de la table de fréquences et du message binaire
        StringBuilder resultat = new StringBuilder();
        
        // Ajouter le nombre de caractères différents (8 bits)
        String nombreCaracteresBin = String.format("%8s", Integer.toBinaryString(frequences.size())).replace(' ', '0');
        resultat.append(nombreCaracteresBin);
        
        // Ajouter chaque caractère et sa fréquence
        for (Map.Entry<Character, Integer> entry : frequences.entrySet()) {
            // Caractère (16 bits)
            String caractereBin = String.format("%16s", Integer.toBinaryString(entry.getKey())).replace(' ', '0');
            resultat.append(caractereBin);
            
            // Fréquence (32 bits)
            String frequenceBin = String.format("%32s", Integer.toBinaryString(entry.getValue())).replace(' ', '0');
            resultat.append(frequenceBin);
        }
        
        // Ajouter la longueur du message binaire (32 bits)
        String longueurMessageBin = String.format("%32s", Integer.toBinaryString(messageBinaire.length())).replace(' ', '0');
        resultat.append(longueurMessageBin);
        
        // Ajouter le message binaire
        resultat.append(messageBinaire);
        
        // Ajouter la longueur totale au début (32 bits)
        String longueurTotaleBin = String.format("%32s", Integer.toBinaryString(resultat.length())).replace(' ', '0');
        
        return longueurTotaleBin + resultat.toString();
    }
    
    // Désérialise la chaîne binaire en message
    private static String deserializerDonnees(String donneesCompletes) {
        try {
            // Vérifier si la chaîne est assez longue pour contenir les données minimales (32 bits pour la longueur totale)
            if (donneesCompletes.length() < 32) {
                throw new IllegalArgumentException("Données incomplètes: moins de 32 bits");
            }
            
            // Extraire la partie après les 32 premiers bits (qui indiquent la longueur totale)
            String donnees = donneesCompletes.substring(32);
            
            // Vérifier si la chaîne est assez longue pour contenir le nombre de caractères (8 bits)
            if (donnees.length() < 8) {
                throw new IllegalArgumentException("Données incomplètes: info sur le nombre de caractères manquante");
            }
            
            // Extraire le nombre de caractères différents
            int nombreCaracteres = Integer.parseInt(donnees.substring(0, 8), 2);
            int position = 8;
            
            // Vérifier si la longueur restante est suffisante pour les caractères et leurs fréquences
            int tailleNecessaire = nombreCaracteres * (16 + 32); // 16 bits par caractère + 32 bits par fréquence
            if (donnees.length() - position < tailleNecessaire) {
                throw new IllegalArgumentException("Données incomplètes: table de fréquences tronquée");
            }
            
            // Reconstruire la table de fréquences
            Map<Character, Integer> frequences = new HashMap<>();
            for (int i = 0; i < nombreCaracteres; i++) {
                char caractere = (char) Integer.parseInt(donnees.substring(position, position + 16), 2);
                position += 16;
                
                int frequence = Integer.parseInt(donnees.substring(position, position + 32), 2);
                position += 32;
                
                frequences.put(caractere, frequence);
            }
            
            // Vérifier s'il reste assez de bits pour la longueur du message (32 bits)
            if (donnees.length() - position < 32) {
                throw new IllegalArgumentException("Données incomplètes: longueur du message manquante");
            }
            
            // Extraire la longueur du message binaire
            int longueurMessage = Integer.parseInt(donnees.substring(position, position + 32), 2);
            position += 32;
            
            // Vérifier si la longueur restante est suffisante pour le message
            if (donnees.length() - position < longueurMessage) {
                throw new IllegalArgumentException("Données incomplètes: message binaire tronqué");
            }
            
            // Extraire le message binaire
            String messageBinaire = donnees.substring(position, position + longueurMessage);
            
            // Reconstruire l'arbre de Huffman
            NoeudHuffman racineHuffman = CodageHuffman.construireArbreHuffman(frequences);
            CodageHuffman.genererCodes(racineHuffman, "");
            
            // Décoder le message
            return CodageHuffman.decoder(messageBinaire);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Erreur de conversion des données binaires: " + e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Erreur d'accès aux données: " + e.getMessage());
        }
    }
} 