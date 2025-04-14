class NoeudHuffman implements Comparable<NoeudHuffman> {
    char caractere;
    int frequence;
    NoeudHuffman gauche, droite;

    public NoeudHuffman(char caractere, int frequence) {
        this.caractere = caractere;
        this.frequence = frequence;
    }

    @Override
    public int compareTo(NoeudHuffman noeud) {
        return this.frequence - noeud.frequence;
    }
}