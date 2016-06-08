package cs276.assignments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Query {

    // Term id -> position in index file
    private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
    // Term id -> document frequency
    private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
    // Doc id -> doc name dictionary
    private static Map<Integer, String> docDict = new TreeMap<Integer, String>();
    // Term -> term id dictionary
    private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
    // Index
    private static BaseIndex index = null;

    /*
     * Write a posting list with a given termID from the file You should seek to
     * the file position of this specific posting list and read it back.
     */
    private static PostingList readPosting(FileChannel fc, int termId) throws IOException {
        if (!posDict.containsKey(termId)) return null;
        return index.readPosting(fc.position(posDict.get(termId)));
    }

    public static void main(String[] args) throws IOException {
        /* Parse command line */
        if (args.length != 2) {
            System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
            return;
        }

        /* Get index */
        String className = "cs276.assignments." + args[0] + "Index";
        try {
            Class<?> indexClass = Class.forName(className);
            index = (BaseIndex) indexClass.newInstance();
        } catch (Exception e) {
            System.err.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

        /* Get index directory */
        String input = args[1];
        File inputdir = new File(input);
        if (!inputdir.exists() || !inputdir.isDirectory()) {
            System.err.println("Invalid index directory: " + input);
            return;
        }

        /* Index file */
        RandomAccessFile indexFile = new RandomAccessFile(new File(input, "corpus.index"), "r");
        FileChannel fc = indexFile.getChannel();

        String line = null;
        /* Term dictionary */
        BufferedReader termReader = new BufferedReader(new FileReader(new File(input, "term.dict")));
        while ((line = termReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            termDict.put(tokens[0], Integer.parseInt(tokens[1]));
        }
        termReader.close();

        /* Doc dictionary */
        BufferedReader docReader = new BufferedReader(new FileReader(new File(input, "doc.dict")));
        while ((line = docReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
        }
        docReader.close();

        /* Posting dictionary */
        BufferedReader postReader = new BufferedReader(new FileReader(new File(input, "posting.dict")));
        while ((line = postReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
            freqDict.put(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[2]));
        }
        postReader.close();

        /* Processing queries */
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        /* For each query */
        while ((line = br.readLine()) != null) {

            boolean notFoundFlag = false;

            String[] tokens = line.split("\\s+");
            Set<Integer> termIds = new HashSet<Integer>();
            for (String token : tokens) {
                if (!termDict.containsKey(token)) {
                    notFoundFlag = true;
                    break;
                }
                termIds.add(termDict.get(token));
            }
            List<Integer> res = null;
            if (!notFoundFlag) {
                List<Integer> termIdsSortedByFreq = new ArrayList<Integer>(termIds);
                Collections.sort(termIdsSortedByFreq, new Comparator<Integer>() {
                    @Override
                    public int compare(Integer termId1, Integer termId2) {
                        return freqDict.get(termId1) - freqDict.get(termId2);
                    }
                });
                for (int termId : termIdsSortedByFreq) {
                    res = res == null ? readPosting(fc, termId).getList() : PostingList.intersect(res, readPosting(fc, termId));
                }
            }
            if (res == null)
                System.out.println("no results found");
            else {
                for (int docId : res) {
                    System.out.println(docDict.get(docId));
                }
            }
        }
        br.close();
        indexFile.close();
    }
}
