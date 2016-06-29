package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sun.applet.Main;

/**
 * This class is used to 1) load training data from files 2) build idf from data
 * collections in PA1.
 */
public class LoadHandler {

	/**
	 * Loads the training data.
	 * 
	 * @param feature_file_name
	 *            the name of the feature file.
	 * @return the mapping of Query-url-Document
	 */
	public static Map<Query, Map<String, Document>> loadTrainData(String feature_file_name) throws Exception {
		File feature_file = new File(feature_file_name);
		if (!feature_file.exists()) {
			System.err.println("Invalid feature file name: " + feature_file_name);
			return null;
		}

		BufferedReader reader = new BufferedReader(new FileReader(feature_file));
		String line = null, url = null, anchor_text = null;
		Query query = null;

		/* Feature dictionary: Query -> (url -> Document) */
		Map<Query, Map<String, Document>> queryDict = new HashMap<Query, Map<String, Document>>();

		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(":", 2);
			String key = tokens[0].trim();
			String value = tokens[1].trim();

			if (key.equals("query")) {
				query = new Query(value);
				queryDict.put(query, new HashMap<String, Document>());
			} else if (key.equals("url")) {
				url = value;
				queryDict.get(query).put(url, new Document(url));
			} else if (key.equals("title")) {
				queryDict.get(query).get(url).title = new String(value);
			} else if (key.equals("header")) {
				if (queryDict.get(query).get(url).headers == null)
					queryDict.get(query).get(url).headers = new ArrayList<String>();
				queryDict.get(query).get(url).headers.add(value);
			} else if (key.equals("body_hits")) {
				if (queryDict.get(query).get(url).body_hits == null)
					queryDict.get(query).get(url).body_hits = new HashMap<String, List<Integer>>();
				String[] temp = value.split(" ", 2);
				String term = temp[0].trim();
				List<Integer> positions_int;

				if (!queryDict.get(query).get(url).body_hits.containsKey(term)) {
					positions_int = new ArrayList<Integer>();
					queryDict.get(query).get(url).body_hits.put(term, positions_int);
				} else
					positions_int = queryDict.get(query).get(url).body_hits.get(term);

				String[] positions = temp[1].trim().split(" ");
				for (String position : positions)
					positions_int.add(Integer.parseInt(position));

			} else if (key.equals("body_length"))
				queryDict.get(query).get(url).body_length = Integer.parseInt(value);
			else if (key.equals("pagerank"))
				queryDict.get(query).get(url).page_rank = Integer.parseInt(value);
			else if (key.equals("anchor_text")) {
				anchor_text = value;
				if (queryDict.get(query).get(url).anchors == null)
					queryDict.get(query).get(url).anchors = new HashMap<String, Integer>();
			} else if (key.equals("stanford_anchor_count"))
				queryDict.get(query).get(url).anchors.put(anchor_text, Integer.parseInt(value));
		}

		reader.close();

		return queryDict;
	}

	/**
	 * Unserializes the term-doc counts from file.
	 * 
	 * @param idfFile
	 *            the file containing the idfs.
	 * @return the mapping of term-doc counts.
	 */
	public static Map<String, Double> loadDFs(String idfFile) {
		Map<String, Double> termDocCount = null;
		try {
			FileInputStream fis = new FileInputStream(idfFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			termDocCount = (HashMap<String, Double>) ois.readObject();
			ois.close();
			fis.close();
		} catch (IOException | ClassNotFoundException ioe) {
			ioe.printStackTrace();
			return null;
		}
		return termDocCount;
	}

	/**
	 * Builds document frequencies and then serializes to file.
	 * 
	 * @param dataDir
	 *            the data directory
	 * @param idfFile
	 *            the file containing the idfs.
	 * @return the term-doc counts
	 * @throws IOException
	 */
	public static Map<String, Double> buildDFs(String dataDir, String idfFile) throws IOException {
		// Get root directory
		String root = dataDir;
		File rootdir = new File(root);
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return null;
		}

		// Array of all the blocks (sub directories) in the PA1 corpus
		File[] dirlist = rootdir.listFiles();
		int totalDocCount = 0;

		// Count number of documents in which each term appears
		Map<String, Integer> termDocCount = new HashMap<String, Integer>();
		Map<String, Double> termIdfValue = new HashMap<String, Double>();

		for (File block : dirlist) {
			if (block.isHidden() || !block.isDirectory()) {
				continue;
			}
			// System.out.println("Start processing block: " + block.getPath());
			for (File file : block.listFiles()) {
				if (file.isHidden())
					continue;
				++totalDocCount;
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().toLowerCase().split("\\s+");
					for (String token : tokens) {
						if (!termDocCount.containsKey(token)) {
							termDocCount.put(token, 0);
						}
						termDocCount.put(token, termDocCount.get(token) + 1);
					}
				}
				reader.close();
			}
		}

		// Compute inverse document frequencies using document frequencies
		for (Map.Entry<String, Integer> entry : termDocCount.entrySet()) {
			termIdfValue.put(entry.getKey(), Math.log((1 + (double) totalDocCount) / (1 + entry.getValue())));
		}
		termIdfValue.put("__NONE__", Math.log(1 + (double) totalDocCount));

		// Save to file
		try {
			FileOutputStream fos = new FileOutputStream(idfFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(termIdfValue);
			oos.close();
			fos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return termIdfValue;
	}

	public static void main(String[] args) throws IOException {
		LoadHandler.buildDFs("/Users/YunchenWei/Documents/Coursera/IR/pa1-data", "term.idf");
	}

}
