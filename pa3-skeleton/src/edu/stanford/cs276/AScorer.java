package edu.stanford.cs276;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An abstract class for a scorer. Needs to be extended by each specific
 * implementation of scorers.
 */
public abstract class AScorer {

	// Map: term -> idf
	Map<String, Double> idfs;

	// Various types of term frequencies that you will need
	String[] TFTYPES = { "url", "title", "body", "header", "anchor" };

	/**
	 * Construct an abstract scorer with a map of idfs.
	 * 
	 * @param idfs
	 *            the map of idf scores
	 */
	public AScorer(Map<String, Double> idfs) {
		this.idfs = idfs;
	}

	/**
	 * Score each document for each query.
	 * 
	 * @param d
	 *            the Document
	 * @param q
	 *            the Query
	 */
	public abstract double getSimScore(Document d, Query q);

	/**
	 * Get frequencies for a query.
	 * 
	 * @param q
	 *            the query to compute frequencies for
	 */
	public Map<String, Double> getQueryFreqs(Query q) {

		// queryWord -> term frequency
		Map<String, Double> tfQuery = new HashMap<String, Double>();

		for (String queryWord : q.queryWords) {
			if (!tfQuery.containsKey(queryWord))
				tfQuery.put(queryWord, 0.0);
			tfQuery.put(queryWord, tfQuery.get(queryWord) + 1);
		}

		for (Map.Entry<String, Double> entry : tfQuery.entrySet()) {
			entry.setValue(1 + Math.log(entry.getValue()));
		}

		return tfQuery;
	}

	private Map<String, Double> initTfMap(Query q) {
		Map<String, Double> tfMap = new HashMap<String, Double>();
		for (String queryWord : q.queryWords) {
			tfMap.put(queryWord, 0.0);
		}
		return tfMap;
	}

	private Map<String, Double> parseDocUrlTermFreq(Document d, Query q) {
		Map<String, Double> urlTfMap = initTfMap(q);
		if (d.url == null)
			return urlTfMap;
		for (String term : d.url.toLowerCase().split("[\\./]")) {
			if (urlTfMap.containsKey(term))
				urlTfMap.put(term, urlTfMap.get(term) + 1);
		}
		return urlTfMap;
	}

	private Map<String, Double> parseDocTitleTermFreq(Document d, Query q) {
		Map<String, Double> titleTfMap = initTfMap(q);
		if (d.title == null)
			return titleTfMap;
		for (String term : d.title.toLowerCase().split("\\s+")) {
			if (titleTfMap.containsKey(term))
				titleTfMap.put(term, titleTfMap.get(term) + 1);
		}
		return titleTfMap;
	}

	private Map<String, Double> parseDocHeaderTermFreq(Document d, Query q) {
		Map<String, Double> headerTfMap = initTfMap(q);
		if (d.headers == null)
			return headerTfMap;
		for (String header : d.headers) {
			for (String term : header.toLowerCase().split("\\s+"))
				if (headerTfMap.containsKey(term))
					headerTfMap.put(term, headerTfMap.get(term) + 1);
		}
		return headerTfMap;
	}

	private Map<String, Double> parseDocBodyTermFreq(Document d, Query q) {
		Map<String, Double> bodyTfMap = initTfMap(q);
		if (d.body_hits == null)
			return bodyTfMap;
		for (Map.Entry<String, List<Integer>> hit : d.body_hits.entrySet()) {
			bodyTfMap.put(hit.getKey(), Double.valueOf(hit.getValue().size()));
		}
		return bodyTfMap;
	}

	private Map<String, Double> parseDocAnchorTermFreq(Document d, Query q) {
		Map<String, Double> anchorTextTfMap = initTfMap(q);
		if (d.anchors == null)
			return anchorTextTfMap;
		for (Entry<String, Integer> anchorCount : d.anchors.entrySet()) {
			String anchor = anchorCount.getKey();
			int count = anchorCount.getValue();
			for (String term : anchor.toLowerCase().split("\\s+")) {
				if (anchorTextTfMap.containsKey(term)) {
					anchorTextTfMap.put(term, anchorTextTfMap.get(term) + count);
				}
			}
		}
		return anchorTextTfMap;
	}

	/**
	 * Accumulate the various kinds of term frequencies for the fields (url,
	 * title, body, header, and anchor). You can override this if you'd like,
	 * but it's likely that your concrete classes will share this
	 * implementation.
	 * 
	 * @param d
	 *            the Document
	 * @param q
	 *            the Query
	 */
	public Map<String, Map<String, Double>> getDocTermFreqs(Document d, Query q) {

		// Map from tf type -> queryWord -> score
		Map<String, Map<String, Double>> tfs = new HashMap<String, Map<String, Double>>();

		tfs.put("url", parseDocUrlTermFreq(d, q));
		tfs.put("title", parseDocTitleTermFreq(d, q));
		tfs.put("body", parseDocBodyTermFreq(d, q));
		tfs.put("header", parseDocHeaderTermFreq(d, q));
		tfs.put("anchor", parseDocAnchorTermFreq(d, q));

		for (Map<String, Double> tf : tfs.values()) {
			for (Entry<String, Double> entry : tf.entrySet()) {
				if (entry.getValue() != 0)
					entry.setValue(1 + Math.log(entry.getValue()));
			}
		}

		return tfs;
	}

}
