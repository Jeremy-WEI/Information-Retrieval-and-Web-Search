package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.stanford.cs276.util.Pair;

/**
 * Implement {@link EditCostModel} interface. Use the query corpus to learn a
 * model of errors that occur in our dataset of queries, and use this to compute
 * P(R|Q)
 */
public class EmpiricalCostModel implements EditCostModel {
    private static final long serialVersionUID = 1L;

    private static final double correctProb = Config.ucmCorrectProb;
    private static final int INSERT = 0, DELETE = 1, SUBSTITUTE = 2, TRANSPOSE = 3;

    private Map<Character, Integer> charCounts = new HashMap<Character, Integer>();
    private Map<String, Integer> bicharCounts = new HashMap<String, Integer>();
    private Map<String, Integer> editCounts = new HashMap<String, Integer>();

    public EmpiricalCostModel(String editsFile) throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(editsFile));
        System.out.println("Constructing edit distance map...");
        String line = null;
        Scanner lineSc = null;
        while ((line = input.readLine()) != null) {
            lineSc = new Scanner(line);
            lineSc.useDelimiter("\t");
            String noisy = "#" + lineSc.next();
            String clean = "#" + lineSc.next();
            for (int i = 0; i < clean.length(); i++) {
                char c = clean.charAt(i);
                if (!charCounts.containsKey(c)) charCounts.put(c, 0);
                charCounts.put(c, charCounts.get(c));
                if (i > 0) {
                    String s = clean.substring(i - 1, i + 1);
                    if (!bicharCounts.containsKey(s)) bicharCounts.put(s, 0);
                    bicharCounts.put(s, bicharCounts.get(s) + 1);
                }
            }
            Pair<Integer, Integer> editTypeIndexPair = stringDiff(noisy, clean);
            String diff = null;
            int index = editTypeIndexPair.getSecond();
            switch (editTypeIndexPair.getFirst()) {
            case INSERT:
                diff = clean.substring(index, index + 1) + "->" + noisy.substring(index, index + 2);
                break;
            case DELETE:
                diff = clean.substring(index, index + 2) + "->" + noisy.substring(index, index + 1);
                break;
            case SUBSTITUTE:
                diff = clean.substring(index, index + 1) + "->" + noisy.substring(index, index + 1);
                break;
            case TRANSPOSE:
                diff = clean.substring(index, index + 2) + "->" + noisy.substring(index, index + 2);
                break;
            }
            if (!editCounts.containsKey(diff)) editCounts.put(diff, 0);
            editCounts.put(diff, editCounts.get(diff) + 1);
            lineSc.close();
        }

        input.close();
        System.out.println("Done.");
    }

    // You need to update this to calculate the proper empirical cost
    @Override
    public double editProbability(String original, String R) {
        original = "#" + original;
        R = "#" + R;
        Pair<Integer, Integer> editTypeIndexPair = stringDiff(original, R);
        if (editTypeIndexPair == null) return correctProb;
        String diff = null;
        int index = editTypeIndexPair.getSecond();
        switch (editTypeIndexPair.getFirst()) {
        case INSERT:
            diff = original.substring(index, index + 1) + "->" + R.substring(index, index + 2);
            return (double) (editCounts.get(diff) + 1) / (charCounts.get(original.charAt(index)) + charCounts.size());
        case DELETE:
            diff = original.substring(index, index + 2) + "->" + R.substring(index, index + 1);
            return (double) (editCounts.get(diff) + 1) / (bicharCounts.get(original.substring(index, index + 2)) + bicharCounts.size());
        case SUBSTITUTE:
            diff = original.substring(index, index + 1) + "->" + R.substring(index, index + 1);
            return (double) (editCounts.get(diff) + 1) / (charCounts.get(original.charAt(index)) + charCounts.size());
        case TRANSPOSE:
            diff = original.substring(index, index + 2) + "->" + R.substring(index, index + 2);
            return (double) (editCounts.get(diff) + 1) / (bicharCounts.get(original.substring(index, index + 2)) + bicharCounts.size());
        }
        return 0.0;
    }

    public Pair<Integer, Integer> stringDiff(String noisy, String clean) {
        if (noisy.equals(clean)) return null;
        //        noisy = "#" + noisy;
        //        clean = "#" + clean;
        int nLen = noisy.length();
        int cLen = clean.length();
        int i = 0;
        while (i < Math.min(nLen, cLen) && noisy.charAt(i) == clean.charAt(i)) {
            i++;
        }
        if (nLen < cLen) {
            return new Pair<Integer, Integer>(DELETE, i - 1);
        }
        if (nLen > cLen) {
            return new Pair<Integer, Integer>(INSERT, i - 1);
        }
        if (i == nLen - 1 || noisy.charAt(i + 1) == clean.charAt(i + 1)) {
            return new Pair<Integer, Integer>(SUBSTITUTE, i);
        }
        return new Pair<Integer, Integer>(TRANSPOSE, i);
    }
}
