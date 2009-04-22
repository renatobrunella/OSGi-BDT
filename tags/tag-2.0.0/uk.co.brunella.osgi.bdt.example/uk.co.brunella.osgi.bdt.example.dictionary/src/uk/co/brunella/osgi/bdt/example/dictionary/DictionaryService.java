package uk.co.brunella.osgi.bdt.example.dictionary;

public interface DictionaryService {

  String lookup(String word);
  String inverseLookup(String word);
}
