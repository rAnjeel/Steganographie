import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class CodageHuffman {
    private static Map<Character, String> codeHuffman = new HashMap<>();
    private static Map<String, Character> codeHuffmanInverse = new HashMap<>();

    // Méthode pour réinitialiser les cartes avant chaque nouvelle génération de codes
    public static void reinitialiserCartes() {
        codeHuffman.clear();
        codeHuffmanInverse.clear();
    }

    public static NoeudHuffman construireArbreHuffman(Map<Character, Integer> carteFrequences) {
        PriorityQueue<NoeudHuffman> filePriorite = new PriorityQueue<>();
        for (var entree : carteFrequences.entrySet()) {
            filePriorite.add(new NoeudHuffman(entree.getKey(), entree.getValue()));
        }

        while (filePriorite.size() > 1) {
            NoeudHuffman gauche = filePriorite.poll();
            NoeudHuffman droite = filePriorite.poll();
            NoeudHuffman nouveauNoeud = new NoeudHuffman('\0', gauche.frequence + droite.frequence);
            nouveauNoeud.gauche = gauche;
            nouveauNoeud.droite = droite;
            filePriorite.add(nouveauNoeud);
        }
        return filePriorite.poll();
    }

    public static void genererCodes(NoeudHuffman racine, String code) {
        // Réinitialiser les cartes avant de générer de nouveaux codes
        reinitialiserCartes();
        
        // Appeler la méthode interne pour générer les codes
        genererCodesInternes(racine, code);
    }
    
    private static void genererCodesInternes(NoeudHuffman racine, String code) {
        if (racine == null) return;
        if (racine.caractere != '\0') {
            codeHuffman.put(racine.caractere, code);
            codeHuffmanInverse.put(code, racine.caractere);
        }
        genererCodesInternes(racine.gauche, code + "0");
        genererCodesInternes(racine.droite, code + "1");
    }

    public static String encoder(String texte) {
        StringBuilder texteEncode = new StringBuilder();
        for (char ch : texte.toCharArray()) {
            if (codeHuffman.containsKey(ch)) {
                texteEncode.append(codeHuffman.get(ch));
            } else {
                // Gérer les caractères qui ne sont pas dans la carte de fréquences
                System.err.println("Avertissement: Le caractère '" + ch + "' n'est pas dans la table de Huffman");
            }
        }
        return texteEncode.toString();
    }

    public static String decoder(String texteEncode) {
        StringBuilder texteDecode = new StringBuilder();
        String temp = "";
        for (char bit : texteEncode.toCharArray()) {
            temp += bit;
            if (codeHuffmanInverse.containsKey(temp)) {
                texteDecode.append(codeHuffmanInverse.get(temp));
                temp = "";
            }
        }
        return texteDecode.toString();
    }

    public static void compresserFichier(String fichierEntree, String fichierSortie) throws IOException {
        String texte = new String(Files.readAllBytes(new File(fichierEntree).toPath()));
        Map<Character, Integer> carteFrequences = new HashMap<>();
        for (char ch : texte.toCharArray()) {
            carteFrequences.put(ch, carteFrequences.getOrDefault(ch, 0) + 1);
        }

        NoeudHuffman racine = construireArbreHuffman(carteFrequences);
        genererCodes(racine, "");
        String texteEncode = encoder(texte);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fichierSortie))) {
            oos.writeObject(carteFrequences);
            oos.writeObject(texteEncode);
        }
    }

    public static void decompresserFichier(String fichierEntree, String fichierSortie) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fichierEntree))) {
            Map<Character, Integer> carteFrequences = (Map<Character, Integer>) ois.readObject();
            String texteEncode = (String) ois.readObject();
            NoeudHuffman racine = construireArbreHuffman(carteFrequences);
            genererCodes(racine, "");
            String texteDecode = decoder(texteEncode);
            Files.write(new File(fichierSortie).toPath(), texteDecode.getBytes());
        }
    }
    
    // Méthode pour afficher les codes Huffman (utile pour le débogage)
    public static void afficherCodes() {
        System.out.println("Table de codage Huffman:");
        for (Map.Entry<Character, String> entry : codeHuffman.entrySet()) {
            System.out.println("'" + entry.getKey() + "' -> " + entry.getValue());
        }
    }
}
