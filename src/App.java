import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.UnsupportedAudioFileException;

public class App {
    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedAudioFileException {
        // Verset biblique à cacher
        String versetBiblique = "Au commencement, Dieu créa les cieux et la terre. - Genèse 1:1";
        
        // Paramètres de sécurité
        String prenom = "Jean";
        int numero = 123456;
        
        // Exemple avec un fichier PNG
        System.out.println("Stéganographie avec un fichier PNG :");
        System.out.println("Message original : " + versetBiblique);
        
        try {
            // // Cacher le message dans une image PNG
            // SteganographiePNG.cacherMessage("images/input.png", "images/output.png", versetBiblique, prenom, numero);
            // System.out.println("Message caché dans images/output.png");
            
            try {
                // Extraire le message caché
                String messageExtrait = SteganographiePNG.extraireMessage("images/output.png", prenom, numero);
                System.out.println("Message extrait : " + messageExtrait);
            } catch (IllegalArgumentException e) {
                System.out.println("Erreur lors de l'extraction du message du PNG : " + e.getMessage());
                System.out.println("La stéganographie pourrait avoir été compromise.");
            }
        } catch (IOException e) {
            System.out.println("Erreur avec le fichier PNG : " + e.getMessage());
            System.out.println("Assurez-vous que le répertoire images/ existe et contient un fichier input.png");
        }
        
        // // Exemple avec un fichier WAV
        // System.out.println("\nStéganographie avec un fichier WAV :");
        // System.out.println("Message original : " + versetBiblique);
        
        // try {
        //     // Cacher le message dans un fichier audio WAV
        //     SteganographieWAV.cacherMessage("audio/input.wav", "audio/output.wav", versetBiblique, prenom, numero);
        //     System.out.println("Message caché dans audio/output.wav");
            
        //     try {
        //         // Extraire le message caché
        //         String messageExtrait = SteganographieWAV.extraireMessage("audio/output.wav", prenom, numero);
        //         System.out.println("Message extrait : " + messageExtrait);
        //     } catch (IllegalArgumentException e) {
        //         System.out.println("Erreur lors de l'extraction du message du WAV : " + e.getMessage());
        //         System.out.println("La stéganographie pourrait avoir été compromise.");
        //     }
        // } catch (IOException | UnsupportedAudioFileException e) {
        //     System.out.println("Erreur avec le fichier WAV : " + e.getMessage());
        //     System.out.println("Assurez-vous que le répertoire audio/ existe et contient un fichier input.wav");
        // }
        
        // Exemple de démonstration du codage Huffman seul
        System.out.println("\nDémonstration du codage Huffman seul :");
        String texte = "God is good";
        
        // Calculer les fréquences
        Map<Character, Integer> frequences = new HashMap<>();
        for (char ch : texte.toCharArray()) {
            frequences.put(ch, frequences.getOrDefault(ch, 0) + 1);
        }
        
        // Construire l'arbre de Huffman
        NoeudHuffman racine = CodageHuffman.construireArbreHuffman(frequences);
        CodageHuffman.genererCodes(racine, "");
        
        // Afficher la table de codage
        CodageHuffman.afficherCodes();
        
        // Encoder et décoder
        String texteCodé = CodageHuffman.encoder(texte);
        System.out.println("Texte original : " + texte);
        System.out.println("Texte codé     : " + texteCodé);
        String texteDécodé = CodageHuffman.decoder(texteCodé);
        System.out.println("Texte décodé   : " + texteDécodé);
        
        // Vérifier si le texte décodé correspond au texte original
        if (texte.equals(texteDécodé)) {
            System.out.println("✓ Le décodage est correct!");
        } else {
            System.out.println("✗ Erreur de décodage! Les textes ne correspondent pas.");
            System.out.println("Différences:");
            for (int i = 0; i < Math.min(texte.length(), texteDécodé.length()); i++) {
                if (texte.charAt(i) != texteDécodé.charAt(i)) {
                    System.out.println("Position " + i + ": '" + texte.charAt(i) + "' vs '" + texteDécodé.charAt(i) + "'");
                }
            }
            if (texte.length() != texteDécodé.length()) {
                System.out.println("Longueurs différentes: " + texte.length() + " vs " + texteDécodé.length());
            }
        }
    }
}
