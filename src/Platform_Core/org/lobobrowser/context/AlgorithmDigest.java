package org.lobobrowser.context;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class AlgorithmDigest implements Comparable<AlgorithmDigest> {

  private final String algorithm;
  private final String digest;
  private final Integer strength;
  private final static Map<String, Integer> str = createMap();

  public AlgorithmDigest(final String algorithm, final String digest, final Integer strength) {
    super();
    this.algorithm = algorithm;
    this.digest = digest;
    this.strength = strength;
  }

  public static boolean validate(final byte[] input, final String integrity) {
    final List<AlgorithmDigest> algDigests = parseMetadata(integrity);
    if (algDigests == null) {
      return true;
    }
    final boolean matchFound = algDigests.stream().anyMatch((algDigest) -> {
      final String encodedResult = algDigest.getHash(input);
      return encodedResult.equals(algDigest.digest);
    });
    return matchFound;
  }

  public static List<AlgorithmDigest> parseMetadata(final String integrity) {
    if (integrity == null || integrity.length() == 0) {
      return null;
    }
    final String[] tokens = integrity.split("\\s+");
    final List<AlgorithmDigest> hashes = getHashes(tokens);
    if (hashes.isEmpty()) {
      return null;
    }
    return strongestAlgDigests(hashes);
  }

  private static List<AlgorithmDigest> getHashes(final String[] tokens) {
    final List<AlgorithmDigest> hashes = new ArrayList<AlgorithmDigest>();
    for (final String token : tokens) {
      // TODO: check if token is valid metadata. Reference: https://github.com/w3c/webappsec/issues/531
      final int hyphen = token.indexOf("-");
      if (hyphen < 0) {
        continue;
      }
      final String alg = token.substring(0, hyphen);
      if (str.containsKey(alg)) {
        final String hashWithOptions = token.substring(hyphen + 1);
        final String hash = parseHashExpression(hashWithOptions);
        final AlgorithmDigest ag = new AlgorithmDigest(alg, hash, str.get(alg));
        hashes.add(ag);
      }
    }
    return hashes;
  }

  private static String parseHashExpression(final String hashWithOptions) {
    final int question = hashWithOptions.indexOf("?");
    return (question < 0) ? hashWithOptions : hashWithOptions.substring(0, question);
  }

  public static List<AlgorithmDigest> strongestAlgDigests(final List<AlgorithmDigest> hashes) {
    Collections.sort(hashes, (a, b) -> a.compareTo(b));
    final Integer strongest = hashes.get(0).strength;
    final List<AlgorithmDigest> result = hashes.stream().filter((h) -> h.strength == strongest)
        .collect(Collectors.toList());
    return result;
  }

  public String getHash(final byte[] input) {
    final String alg = algorithm.substring(0, 3) + "-" + algorithm.substring(3);
    try {
      final MessageDigest md = MessageDigest.getInstance(alg); // returns an object that implements alg
      md.update(input); // updates digest using input array of bytes
      final byte[] digestedBytes = md.digest(); // completes hash, returns array of bytes
      return Base64.getEncoder().encodeToString(digestedBytes); // converts array of bytes into base64-encoded string
    } catch (final Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
    return null;
  }

  private static Map<String, Integer> createMap() {
    final Map<String, Integer> stren = new HashMap<String, Integer>();
    stren.put("sha256", 1);
    stren.put("sha384", 2);
    stren.put("sha512", 3);
    return stren;
  }

  @Override
  public int compareTo(final AlgorithmDigest o) {
    return o.strength - strength;
  }

  @Override
  public String toString() {
    return "AlgorithmDigest [algorithm=" + algorithm + ", digest=" + digest + ", strength=" + strength + "]";
  }

}
