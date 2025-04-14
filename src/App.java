import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.UnsupportedAudioFileException;

public class App {
    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedAudioFileException {
        // Message à cacher
        String message = "Adam and Eve were the first humans and they were the first to eat the apple"; // Message simple pour le test
        
        System.out.println("=== STÉGANOGRAPHIE AVEC MÉTHODE SIMPLIFIÉE ===");
        System.out.println("Message original : " + message);
        
        // // Test avec PNG
        // try {
        //     // Cacher le message dans une image PNG
        //     System.out.println("\n1. MASQUAGE DU MESSAGE DANS UNE IMAGE PNG");
        //     try {
        //         SteganographieSimple.cacher("images/input.png", "images/simple_output.png", message);
        //         System.out.println("Message caché dans images/simple_output.png");
        //     } catch (IOException e) {
        //         System.out.println("Erreur lors du masquage du message dans l'image : " + e.getMessage());
        //         System.out.println("Vérifiez que le fichier input.png existe dans le répertoire images/");
        //     }
            
        //     // Extraire le message
        //     System.out.println("\n2. EXTRACTION DU MESSAGE CACHÉ DE L'IMAGE PNG");
        //     try {
        //         String messageExtrait = SteganographieSimple.extraire("images/simple_output.png");
        //         System.out.println("Message extrait : '" + messageExtrait + "'");
                
        //         if (message.equals(messageExtrait)) {
        //             System.out.println("✓ L'extraction a réussi ! Le message extrait correspond au message original.");
        //         } else {
        //             System.out.println("✗ L'extraction a échoué, le message ne correspond pas exactement.");
        //             System.out.println("Vérifiez les paramètres de la stéganographie.");
        //         }
        //     } catch (Exception e) {
        //         System.out.println("Erreur lors de l'extraction du message de l'image : " + e.getMessage());
        //         e.printStackTrace();
        //     }
        // } catch (Exception e) {
        //     System.out.println("Erreur générale avec l'image: " + e.getMessage());
        //     e.printStackTrace();
        // }
        
        // Test avec WAV
        try {
            // Cacher le message dans un fichier WAV
            System.out.println("\n3. MASQUAGE DU MESSAGE DANS UN FICHIER WAV");
            try {
                SteganographieWAVSimple.cacher("audio/input.wav", "audio/simple_output2.wav", message);
                System.out.println("Message caché dans audio/simple_output2.wav");
            } catch (IOException | UnsupportedAudioFileException e) {
                System.out.println("Erreur lors du masquage du message dans le WAV : " + e.getMessage());
                System.out.println("Vérifiez que le fichier input.wav existe dans le répertoire audio/");
            }
            
            // Extraire le message
            System.out.println("\n4. EXTRACTION DU MESSAGE CACHÉ DU FICHIER WAV");
            try {
                String messageExtrait = SteganographieWAVSimple.extraire("audio/simple_output2.wav");
                System.out.println("Message extrait : '" + messageExtrait + "'");
                
                if (message.equals(messageExtrait)) {
                    System.out.println("✓ L'extraction a réussi ! Le message extrait correspond au message original.");
                } else {
                    System.out.println("✗ L'extraction a échoué, le message ne correspond pas exactement.");
                    System.out.println("Vérifiez les paramètres de la stéganographie.");
                }
            } catch (Exception e) {
                System.out.println("Erreur lors de l'extraction du message du WAV : " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Erreur générale avec le WAV: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Démonstration du codage Huffman seul pour vérification
        System.out.println("\n=== DÉMONSTRATION DU CODAGE HUFFMAN ===");
        String texteTest = "hello world";
        
        // Calculer les fréquences
        Map<Character, Integer> frequences = new HashMap<>();
        for (char ch : texteTest.toCharArray()) {
            frequences.put(ch, frequences.getOrDefault(ch, 0) + 1);
        }
        
        // Construire l'arbre de Huffman
        NoeudHuffman racine = CodageHuffman.construireArbreHuffman(frequences);
        CodageHuffman.genererCodes(racine, "");
        
        // Afficher la table de codage
        CodageHuffman.afficherCodes();
        
        // Encoder et décoder
        String texteCodé = CodageHuffman.encoder(texteTest);
        System.out.println("Texte original : " + texteTest);
        System.out.println("Texte codé     : " + texteCodé);
        String texteDécodé = CodageHuffman.decoder(texteCodé);
        System.out.println("Texte décodé   : " + texteDécodé);
    }
}
