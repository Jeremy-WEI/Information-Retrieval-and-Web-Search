package cs276.assignments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import cs276.util.Pair;

public class Index {

    // Term id -> (position in index file, doc frequency) dictionary
    private static Map<Integer, Pair<Long, Integer>> postingDict = new TreeMap<Integer, Pair<Long, Integer>>();
    // Doc name -> doc id dictionary
    private static Map<String, Integer> docDict = new TreeMap<String, Integer>();
    // Term -> term id dictionary
    private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
    // Block queue
    private static LinkedList<File> blockQueue = new LinkedList<File>();

    // Total file counter
    private static int totalFileCount = 0;
    // Document counter
    private static int docIdCounter = 0;
    // Term counter
    private static int wordIdCounter = 0;
    // Index
    private static BaseIndex index = null;

    /*
     * Write a posting list to the given file You should record the file
     * position of this posting list so that you can read it back during
     * retrieval
     */
    private static void writePosting(FileChannel fc, PostingList posting) throws IOException {
        if (blockQueue.isEmpty()) {
            postingDict.put(posting.getTermId(), new Pair<>(fc.position(), posting.getList().size()));
        }
        index.writePosting(fc, posting);
    }

    public static void main(String[] args) throws IOException {
        /* Parse command line */
        if (args.length != 3) {
            System.err.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
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

        /* Get root directory */
        String root = args[1];
        File rootdir = new File(root);
        if (!rootdir.exists() || !rootdir.isDirectory()) {
            System.err.println("Invalid data directory: " + root);
            return;
        }

        /* Get output directory */
        String output = args[2];
        File outdir = new File(output);
        if (outdir.exists() && !outdir.isDirectory()) {
            System.err.println("Invalid output directory: " + output);
            return;
        }

        if (!outdir.exists()) {
            if (!outdir.mkdirs()) {
                System.err.println("Create output directory failure");
                return;
            }
        }

        /* BSBI indexing algorithm */
        File[] dirlist = rootdir.listFiles();

        /* For each block */
        for (File block : dirlist) {
            File blockFile = new File(output, block.getName());

            if (blockFile.isHidden()) {
                continue; // skip hidden files. break is probably wrong, I'd continue.
            }

            blockQueue.add(blockFile);

            File blockDir = new File(root, block.getName());
            File[] filelist = blockDir.listFiles();

            TreeMap<Integer, TreeSet<Integer>> blockMap = new TreeMap<Integer, TreeSet<Integer>>();

            /* For each file */
            for (File file : filelist) {
                if (file.isHidden()) {
                    continue; // skip hidden files. break is probably wrong, I'd continue.
                }
                ++totalFileCount;
                String fileName = block.getName() + "/" + file.getName();

                int docId = docIdCounter++;
                docDict.put(fileName, docId);

                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.trim().split("\\s+");
                    for (String token : tokens) {
                        if (!termDict.containsKey(token)) {
                            termDict.put(token, wordIdCounter++);
                        }
                        int termId = termDict.get(token);
                        if (!blockMap.containsKey(termId)) {
                            blockMap.put(termId, new TreeSet<Integer>());
                        }
                        blockMap.get(termId).add(docId);
                    }
                }
                reader.close();
            }

            /* Sort and output */
            if (!blockFile.createNewFile()) {
                System.err.println("Create new block failure.");
                return;
            }

            RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
            FileChannel fc = bfc.getChannel();

            for (int termId : blockMap.keySet()) {
                writePosting(fc, new PostingList(termId, new ArrayList<Integer>(blockMap.get(termId))));
            }

            bfc.close();
        }

        /* Required: output total number of files. */
        System.out.println(totalFileCount);

        /* Merge blocks */
        while (true) {
            if (blockQueue.size() <= 1) break;

            File b1 = blockQueue.removeFirst();
            File b2 = blockQueue.removeFirst();

            File combfile = new File(output, b1.getName() + "+" + b2.getName());
            if (!combfile.createNewFile()) {
                System.err.println("Create new block failure.");
                return;
            }

            RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
            RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
            RandomAccessFile mf = new RandomAccessFile(combfile, "rw");

            FileChannel bfc1 = bf1.getChannel();
            FileChannel bfc2 = bf2.getChannel();
            FileChannel mfc = mf.getChannel();
            PostingList p1, p2;

            do {
                p1 = index.readPosting(bfc1);
                p2 = index.readPosting(bfc2);
                while (p1 != null && (p2 == null || p1.getTermId() < p2.getTermId())) {
                    writePosting(mfc, p1);
                    p1 = index.readPosting(bfc1);
                }
                while (p2 != null && (p1 == null || p2.getTermId() < p1.getTermId())) {
                    writePosting(mfc, p2);
                    p2 = index.readPosting(bfc2);
                }
                if (p1 != null && p2 != null && p1.getTermId() == p2.getTermId()) {
                    PostingList p = PostingList.merge(p1, p2);
                    writePosting(mfc, p);
                }
            } while (p1 != null || p2 != null);

            bf1.close();
            bf2.close();
            mf.close();
            b1.delete();
            b2.delete();
            blockQueue.add(combfile);
        }

        /* Dump constructed index back into file system */
        File indexFile = blockQueue.removeFirst();
        indexFile.renameTo(new File(output, "corpus.index"));

        BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(output, "term.dict")));
        for (String term : termDict.keySet()) {
            termWriter.write(term + "\t" + termDict.get(term) + "\n");
        }
        termWriter.close();

        BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(output, "doc.dict")));
        for (String doc : docDict.keySet()) {
            docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
        }
        docWriter.close();

        BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(output, "posting.dict")));
        for (Integer termId : postingDict.keySet()) {
            postWriter.write(termId + "\t" + postingDict.get(termId).getFirst() + "\t" + postingDict.get(termId).getSecond() + "\n");
        }
        postWriter.close();
    }
}
