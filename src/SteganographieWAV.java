import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SteganographieWAV {
    
    // Cache le message dans le fichier audio
    public static void cacherMessage(String cheminAudio, String cheminSortie, String message, String prenom, int numero) 
            throws IOException, UnsupportedAudioFileException {
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
        
        // Lire le fichier audio
        File fichierAudio = new File(cheminAudio);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fichierAudio);
        
        // Obtenir le format audio
        AudioFormat format = audioInputStream.getFormat();
        
        // Lire les données audio
        byte[] donneesAudio = lireDonneesAudio(audioInputStream);
        
        // Générer un générateur de nombres aléatoires basé sur le prénom et le numéro
        Random random = initialiserRandom(prenom, numero);
        
        // Cacher le message dans les données audio
        int totalBits = donneesCompletes.length();
        
        // Vérifier si le fichier audio est assez grand pour contenir le message
        if (totalBits > donneesAudio.length) {
            throw new IOException("Le fichier audio est trop petit pour contenir le message.");
        }
        
        // Utiliser un tableau pour suivre les positions déjà utilisées
        boolean[] positionsUtilisees = new boolean[donneesAudio.length];
        
        for (int i = 0; i < totalBits; i++) {
            // Générer une position aléatoire
            int position = random.nextInt(donneesAudio.length);
            
            // Si cette position a déjà été utilisée, en trouver une autre
            while (positionsUtilisees[position]) {
                position = random.nextInt(donneesAudio.length);
            }
            positionsUtilisees[position] = true;
            
            // Remplacer le bit le moins significatif du sample audio
            char bit = donneesCompletes.charAt(i);
            int valeurBit = (bit == '1') ? 1 : 0;
            donneesAudio[position] = (byte) ((donneesAudio[position] & 0xFE) | valeurBit);
        }
        
        // Créer un nouveau flux audio avec les données modifiées
        ByteArrayInputStream bais = new ByteArrayInputStream(donneesAudio);
        AudioInputStream nouveauAudioInputStream = new AudioInputStream(bais, format, donneesAudio.length / format.getFrameSize());
        
        // Enregistrer le fichier audio modifié
        AudioSystem.write(nouveauAudioInputStream, AudioFileFormat.Type.WAVE, new File(cheminSortie));
        
        System.out.println("Message caché avec succès. Bits utilisés: " + totalBits + " sur " + donneesAudio.length + " disponibles.");
    }
    
    // Extrait le message caché du fichier audio
    public static String extraireMessage(String cheminAudio, String prenom, int numero) 
            throws IOException, UnsupportedAudioFileException, ClassNotFoundException {
        // Lire le fichier audio
        File fichierAudio = new File(cheminAudio);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fichierAudio);
        
        // Lire les données audio
        byte[] donneesAudio = lireDonneesAudio(audioInputStream);
        
        // Générer un générateur de nombres aléatoires basé sur le prénom et le numéro
        Random random = initialiserRandom(prenom, numero);
        
        // Générer les mêmes positions aléatoires que lors du masquage pour extraire les 32 premiers bits (longueur totale)
        StringBuilder longueurBinaire = new StringBuilder();
        boolean[] positionsUtilisees = new boolean[donneesAudio.length];
        
        // Extraire les 32 premiers bits qui représentent la longueur totale
        for (int i = 0; i < 32; i++) {
            // Générer une position aléatoire
            int position = random.nextInt(donneesAudio.length);
            
            // Si cette position a déjà été utilisée, en trouver une autre
            while (positionsUtilisees[position]) {
                position = random.nextInt(donneesAudio.length);
            }
            positionsUtilisees[position] = true;
            
            int bit = donneesAudio[position] & 1;
            longueurBinaire.append(bit);
        }
        
        try {
            // Convertir la chaîne binaire en un entier - avec une valeur maximale pour éviter les erreurs
            int longueurTotale = Integer.parseInt(longueurBinaire.toString(), 2);
            
            // Limiter la longueur pour éviter les dépassements
            longueurTotale = Math.min(longueurTotale, donneesAudio.length);
            
            // Si la longueur totale est absurde, c'est probablement une erreur
            if (longueurTotale <= 0 || longueurTotale > donneesAudio.length / 2) {
                throw new IllegalArgumentException("Longueur de données invalide: " + longueurTotale);
            }
            
            System.out.println("Longueur des données à extraire: " + longueurTotale + " bits");
            
            // Extraire les données complètes
            StringBuilder donneesCompletes = new StringBuilder(longueurBinaire.toString());
            
            // Réinitialiser le générateur aléatoire et les positions utilisées pour extraire le reste des données
            random = initialiserRandom(prenom, numero);
            positionsUtilisees = new boolean[donneesAudio.length];
            
            // Refaire le même parcours en commençant par extraire les 32 premiers bits (longueur)
            for (int i = 0; i < 32; i++) {
                int position = random.nextInt(donneesAudio.length);
                
                while (positionsUtilisees[position]) {
                    position = random.nextInt(donneesAudio.length);
                }
                positionsUtilisees[position] = true;
            }
            
            // Puis extraire le reste des données
            for (int i = 32; i < longueurTotale; i++) {
                // Générer une position aléatoire
                int position = random.nextInt(donneesAudio.length);
                
                // Si cette position a déjà été utilisée, en trouver une autre
                while (positionsUtilisees[position]) {
                    position = random.nextInt(donneesAudio.length);
                }
                positionsUtilisees[position] = true;
                
                int bit = donneesAudio[position] & 1;
                donneesCompletes.append(bit);
            }
            
            // Désérialiser les données
            return deserializerDonnees(donneesCompletes.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La stéganographie a échoué: impossible de lire la longueur des données: " + e.getMessage());
        }
    }
    
    // Lire les données audio en tant que tableau d'octets
    private static byte[] lireDonneesAudio(AudioInputStream ais) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        
        while ((bytesRead = ais.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        return baos.toByteArray();
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