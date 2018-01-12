package aup.lenskit.example;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.grouplens.lenskit.data.text.Formats;
import org.grouplens.lenskit.data.text.TextEventDAO;
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity;
import org.grouplens.lenskit.vectors.similarity.PearsonCorrelation;
import org.grouplens.lenskit.vectors.similarity.VectorSimilarity;
import org.lenskit.LenskitConfiguration;
import org.lenskit.LenskitRecommender;
import org.lenskit.LenskitRecommenderEngine;
import org.lenskit.api.ItemRecommender;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.RatingPredictor;
import org.lenskit.api.Result;
import org.lenskit.api.ResultList;
import org.lenskit.data.dao.EventDAO;
import org.lenskit.data.dao.ItemNameDAO;
import org.lenskit.data.dao.MapItemNameDAO;
import org.lenskit.knn.NeighborhoodSize;
import org.lenskit.knn.item.ItemItemScorer;
import org.lenskit.knn.item.ItemSimilarity;
import org.lenskit.knn.user.UserUserItemScorer;

public class LenskitTester {
	
	public static void main(String[] args) {
		try {
			File ratingsFile = new File("data/ratings.csv");
			File movieFile = new File("data/movies.csv");
			List<Long> users = new ArrayList<Long>();
			List<Long> movies = new ArrayList<Long>();

			// default config values
			Class<? extends ItemScorer> itemScorer = UserUserItemScorer.class;
			Class<? extends VectorSimilarity> vectorSimilarity = CosineVectorSimilarity.class;
			int neighborhoodSize = 30; 
			int ratingsCount = 50;
			
			LenskitRecommenderEngine engine = null;
			LenskitRecommender rec = null;
			ItemNameDAO names = null;

			printCommands();

			boolean exit = false;

			Scanner sc = new Scanner(System.in);

			do {
				System.out.print("Kommando: ");
				String command = sc.nextLine();

				switch (command) {
				case "I":
					itemScorer = ItemItemScorer.class;
					break;

				case "U":
					itemScorer = UserUserItemScorer.class;
					break;

				case "VSC":
					vectorSimilarity = CosineVectorSimilarity.class;
					break;

				case "VSP":
					vectorSimilarity = PearsonCorrelation.class;
					break;

				case "N":
					System.out.print("Anzahl Nachbarn: ");
					neighborhoodSize = (int) Integer.parseInt(sc.nextLine());
					break;

				case "C":
					EventDAO dao = TextEventDAO.create(ratingsFile, Formats.movieLensLatest());
					names = MapItemNameDAO.fromCSVFile(movieFile, 1);

					LenskitConfiguration config = new LenskitConfiguration();
					config.addComponent(dao);
					config.bind(ItemScorer.class).to(itemScorer);
					config.within(ItemSimilarity.class).bind(VectorSimilarity.class).to(vectorSimilarity);
					config.set(NeighborhoodSize.class).to(neighborhoodSize);

					engine = LenskitRecommenderEngine.build(config);
					rec = engine.createRecommender();
					System.out.println("Konfiguration erstellt!");
					break;

				case "O":
					System.out.println("Anzahl Nachbarn: " + neighborhoodSize);
					System.out.println("Benutzer: " + StringUtils.join(users));
					System.out.println("Filme: " + StringUtils.join(movies));
					System.out.println("ItemScorer: " + itemScorer.getSimpleName());
					System.out.println("VectorSimilarity: " + vectorSimilarity.getSimpleName());
					break;

				case "E":
					System.out.print("Anzahl Vorschläge: ");
					ratingsCount = (int) Integer.parseInt(sc.nextLine());

					ItemRecommender irec = rec.getItemRecommender();

					for (long userId : users) {
						ResultList recs = irec.recommendWithDetails(userId, ratingsCount, null, null);

						System.out.format("Recommendations for user %d:\n", userId);
						for (Result item : recs) {
							String name = names.getItemName(item.getId());
							System.out.format("\t%d (%s): %.2f\n", item.getId(), name, item.getScore());
						}
					}

					break;

				case "P":
					RatingPredictor pred = rec.getRatingPredictor();

					for (long userId : users) {
						Map<Long, Double> res = pred.predict(userId, movies);

						System.out.format("Prediction for user %s: %s \n", userId, res);

						for (Map.Entry<Long, Double> entry : res.entrySet()) {
							System.out.println(entry.getKey() + " => " + entry.getValue());
						}
					}
					break;

				case "UADD":
					System.out.print("Benutzer ID: ");
					users.add(Long.parseLong(sc.nextLine()));
					break;

				case "MADD":
					System.out.print("Movie ID: ");
					movies.add(Long.parseLong(sc.nextLine()));
					break;

				case "UCLEAR":
					users.clear();
					break;

				case "X":
					exit = true;
					sc.close();
					break;

				}
			} while (!exit);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void printCommands() {
		System.out.println("I – Umschalten auf Item-based Ansatz");
		System.out.println("U – Umschalten auf User-based Ansatz");
		System.out.println("VSC – Kosinus Vector Ähnlichkeit");
		System.out.println("VSP – Pearson Ähnlichkeit");
		System.out.println("N – Festlegen der Anzahl an berücksichtigten Nachbarn");
		System.out.println("C – Konfiguration neu erstellen und Datendatei neu lesen");
		System.out.println(
				"O – Ausgabe aller relevanten Informationen zur aktuellen Konfiguration (ausgewählter Ansatz, diverse getätigte Konfigurationen,..)");
		System.out.println(
				"E – Ausführen des Empfehlungsalgorithmus mit vorher festgelegter Konfiguration (Ausgabe N Vorschläge – N vorher abfragen)");
		System.out.println("P – Prediction für definierte Filme ausführen und ausgeben");
		System.out.println("UADD – Benutzer zur Liste für Vorschläge hinzufügen");
		System.out.println("MADD – Film zur Filmliste für Prediction hinzufügen");
		System.out.println("UCLEAR – Benutzerliste leeren");
		System.out.println("X – Programm beenden");
		System.out.println("------------------------------------");
	}

}
