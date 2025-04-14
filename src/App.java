import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.UnsupportedAudioFileException;

public class App {
    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedAudioFileException {
        // Message à cacher
        String message = "Adam and Eve were the first humans"; // Message très court et simple pour le test
        
        System.out.println("=== STÉGANOGRAPHIE AVEC MÉTHODE SIMPLIFIÉE ===");
        System.out.println("Message original : " + message);
        
        try {
            // Cacher le message dans une image PNG
            System.out.println("\n1. Masquage du message dans l'image");
            try {
                SteganographieSimple.cacher("images/input.png", "images/simple_output2.png", message);
                System.out.println("Message caché dans images/simple_output.png");
            } catch (IOException e) {
                System.out.println("Erreur lors du masquage du message : " + e.getMessage());
                System.out.println("Vérifiez que le fichier input.png existe dans le répertoire images/");
                return;
            }
            
            // Extraire le message
            System.out.println("\n2. Extraction du message caché");
            try {
                String messageExtrait = SteganographieSimple.extraire("images/simple_output.png");
                System.out.println("Message extrait : '" + messageExtrait + "'");
                
                // if (message.equals(messageExtrait)) {
                //     System.out.println("✓ L'extraction a réussi ! Le message extrait correspond au message original.");
                // } else {
                //     System.out.println("✗ L'extraction a échoué, le message ne correspond pas exactement.");
                //     System.out.println("Vérifiez les paramètres de la stéganographie.");
                // }
            } catch (Exception e) {
                System.out.println("Erreur lors de l'extraction du message : " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Erreur générale : " + e.getMessage());
            e.printStackTrace();
        }
        
        // Démonstration du codage Huffman seul pour vérification
        System.out.println("\n=== DÉMONSTRATION DU CODAGE HUFFMAN ===");
        String texteTest = "hello";
        
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
