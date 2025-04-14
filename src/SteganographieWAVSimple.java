import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Implémentation simplifiée de stéganographie dans les fichiers WAV
 * en utilisant le codage Huffman et LSB
 */
public class SteganographieWAVSimple {
    
    /**
     * Cache un message dans un fichier WAV en utilisant le LSB
     */
    public static void cacher(String cheminAudio, String cheminSortie, String message) 
        throws IOException, UnsupportedAudioFileException {
        
        // 1. Préparer le message avec codage Huffman
        byte[] messageBytes = preparerMessage(message);
        System.out.println("Message préparé: " + messageBytes.length + " octets");
        
        // 2. Charger le fichier audio
        File fichierAudio = new File(cheminAudio);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fichierAudio);
        AudioFormat formatAudio = audioInputStream.getFormat();
        
        // Informations sur le fichier audio
        int frameSize = formatAudio.getFrameSize();
        int sampleSizeInBits = formatAudio.getSampleSizeInBits();
        int channels = formatAudio.getChannels();
        float sampleRate = formatAudio.getSampleRate();
        
        System.out.println("Informations audio: frameSize=" + frameSize + 
                           ", sampleSize=" + sampleSizeInBits + 
                           ", channels=" + channels + 
                           ", sampleRate=" + sampleRate);
        
        // 3. Lire les données audio dans un tableau d'octets
        byte[] audioBytes = audioInputStream.readAllBytes();
        audioInputStream.close();
        System.out.println("Taille du fichier audio: " + audioBytes.length + " octets");
        
        // 4. Vérifier que le fichier audio est assez grand
        // On a besoin de 8 bits pour 1 bit de message (on utilise seulement 1 bit par octet)
        if ((messageBytes.length * 8 * 8) > audioBytes.length) {
            throw new IOException("Fichier audio trop petit pour le message (" + messageBytes.length + " octets)");
        }
        
        // 5. Cacher les octets du message dans les LSBs des échantillons audio
        // D'abord, cacher la taille du message (32 bits = int)
        int taille = messageBytes.length;
        for (int i = 0; i < 32; i++) {
            int bit = (taille >> i) & 1;
            // Modifier le LSB d'un octet tous les 'frameSize' octets pour éviter la distorsion
            int position = i * frameSize;
            if (position < audioBytes.length) {
                audioBytes[position] = (byte) ((audioBytes[position] & 0xFE) | bit);
            }
        }
        
        // Ensuite, cacher les octets du message
        int messageIndex = 0;
        int bitIndex = 0;
        
        for (int i = 0; i < messageBytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                int bit = (messageBytes[i] >> j) & 1;
                // Position de départ après les 32 bits de la taille
                int position = (32 + (i * 8) + j) * frameSize;
                
                if (position < audioBytes.length) {
                    audioBytes[position] = (byte) ((audioBytes[position] & 0xFE) | bit);
                }
            }
        }
        
        // 6. Recréer un flux audio et enregistrer le fichier
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream nouvelAudioInputStream = new AudioInputStream(
            bais, formatAudio, audioBytes.length / formatAudio.getFrameSize());
        
        AudioSystem.write(nouvelAudioInputStream, AudioFileFormat.Type.WAVE, new File(cheminSortie));
        nouvelAudioInputStream.close();
        
        System.out.println("Message de " + messageBytes.length + " octets caché avec succès dans " + cheminSortie);
    }
    
    /**
     * Extrait un message caché dans un fichier WAV
     */
    public static String extraire(String cheminAudio) 
        throws IOException, UnsupportedAudioFileException {
        
        // 1. Charger le fichier audio
        File fichierAudio = new File(cheminAudio);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fichierAudio);
        AudioFormat formatAudio = audioInputStream.getFormat();
        
        // Informations sur le fichier audio
        int frameSize = formatAudio.getFrameSize();
        int sampleSizeInBits = formatAudio.getSampleSizeInBits();
        int channels = formatAudio.getChannels();
        
        System.out.println("Informations audio pour extraction: frameSize=" + frameSize + 
                           ", sampleSize=" + sampleSizeInBits + 
                           ", channels=" + channels);
        
        // 2. Lire les données audio dans un tableau d'octets
        byte[] audioBytes = audioInputStream.readAllBytes();
        audioInputStream.close();
        System.out.println("Taille du fichier audio: " + audioBytes.length + " octets");
        
        // 3. D'abord, récupérer la taille du message (32 bits = int)
        int taille = 0;
        for (int i = 0; i < 32; i++) {
            int position = i * frameSize;
            if (position < audioBytes.length) {
                int bit = audioBytes[position] & 1;
                taille |= (bit << i);
            }
        }
        
        System.out.println("Taille du message à extraire: " + taille + " octets");
        
        // Vérifier si la taille est plausible
        if (taille <= 0 || taille > (audioBytes.length / (8 * frameSize))) {
            System.out.println("Taille de message invraisemblable, utilisation d'une valeur par défaut");
            taille = 100; // Valeur par défaut
        }
        
        // 4. Récupérer les octets du message
        byte[] messageBytes = new byte[taille];
        for (int i = 0; i < taille; i++) {
            for (int j = 0; j < 8; j++) {
                int position = (32 + (i * 8) + j) * frameSize;
                if (position < audioBytes.length) {
                    int bit = audioBytes[position] & 1;
                    messageBytes[i] |= (bit << j);
                }
            }
        }
        
        // 5. Décoder le message
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
            System.out.println("Erreur lors de la préparation du message WAV: " + e.getMessage());
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
            System.out.println("Erreur lors du décodage du message WAV: " + e.getMessage());
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