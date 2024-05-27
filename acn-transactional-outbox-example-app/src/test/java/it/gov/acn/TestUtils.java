package it.gov.acn;

import it.gov.acn.model.Constituency;
import java.util.UUID;

public class TestUtils {
  private final static String[] constiutencyNames = {"Enel", "Fastweb", "TIM", "Vodafone", "Unicredit"};


  public static Constituency createTestConstituency(){
    Constituency constituency = new Constituency();
    constituency.setId(UUID.randomUUID());
    constituency.setName(randomConstituencyName());
    constituency.setAddress("Via Roma 1");
    return constituency;
  }

  private static String randomConstituencyName(){
    return constiutencyNames[(int) (Math.random() * constiutencyNames.length)];
  }
}
