package edu.stanford.cs276;

/**
 * Implement {@link EditCostModel} interface by assuming assuming that any
 * single edit in the Damerau-Levenshtein distance is equally likely, i.e.,
 * having the same probability
 */
public class UniformCostModel implements EditCostModel {

    private static final long serialVersionUID = 1L;
    private static final double errorProb = Config.ucmErrorProb;
    private static final double correctProb = Config.ucmCorrectProb;

    private int getMinimum(int... nums) {
        int min = Integer.MAX_VALUE;
        for (int num : nums) {
            min = Math.min(num, min);
        }
        return min;
    }

    private int editDistance(String term1, String term2) {
        int len1 = term1.length();
        int len2 = term2.length();
        int dist[][] = new int[len1 + 1][len2 + 1];
        for (int i = 0; i <= len1; i++) {
            dist[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dist[0][j] = j;
        }
        for (int i = 0; i < len1; i++) {
            char c1 = term1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = term2.charAt(j);
                if (c1 == c2) {
                    dist[i + 1][j + 1] = dist[i][j];
                } else {
                    int substiution = dist[i][j] + 1;
                    int deletion = dist[i + 1][j] + 1;
                    int insertion = dist[i][j + 1] + 1;
                    int transposition = (j >= 1 && i >= 1 && term1.charAt(i) == term2.charAt(j - 1) && term1.charAt(i - 1) == term2.charAt(j)) ? dist[i - 1][j - 1] + 1
                            : Integer.MAX_VALUE;
                    dist[i + 1][j + 1] = getMinimum(substiution, deletion, insertion, transposition);
                }
            }
        }
        return dist[len1][len2];
    }

    @Override
    public double editProbability(String original, String R) {
        if (original.equals(R)) return correctProb;
        int editDistance = editDistance(original, R);
        return Math.pow(errorProb, editDistance);
    }

    public static void main(String[] args) {
        UniformCostModel ucm = new UniformCostModel();
        System.out.println(ucm.editProbability("abc", "abcd"));
    }
}
