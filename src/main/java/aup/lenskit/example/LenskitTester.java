package aup.lenskit.example;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.grouplens.lenskit.data.text.Formats;
import org.grouplens.lenskit.data.text.TextEventDAO;
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity;
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
import org.lenskit.knn.item.ItemSimilarity;
import org.lenskit.knn.user.UserUserItemScorer;

public class LenskitTester
{
    public static void main(String[] args)
    {
        try
        {
            File ratingsFile = new File("data/ratings.csv");
            File movieFile = new File("data/movies.csv");
            //List<Long> users = new ArrayList<Long>();
            List<Long> items = new ArrayList<Long>();
            
			int neighborhoodSize  = 30;
            int ratingsCount = 50;
            int predictionUser = 72;
            
            items.add((long)837);
            
            EventDAO dao = TextEventDAO.create(ratingsFile, Formats.movieLensLatest());
            ItemNameDAO names = MapItemNameDAO.fromCSVFile(movieFile, 1);

            // Configure Lenskit
            LenskitConfiguration config = new LenskitConfiguration();
            config.addComponent(dao);
            config.bind(ItemScorer.class).to(UserUserItemScorer.class);
            config.within(ItemSimilarity.class).bind(VectorSimilarity.class).to(CosineVectorSimilarity.class);
            
            config.set(NeighborhoodSize.class).to(neighborhoodSize);
            
            LenskitRecommenderEngine engine = LenskitRecommenderEngine.build(config);
            
			/* Init Recommender and Predictor */
            LenskitRecommender rec = engine.createRecommender();
			RatingPredictor pred = rec.getRatingPredictor();
			
			/* Prediction */			
			Map<Long,Double> res = pred.predict(predictionUser, items);

			System.out.format("Prediction for user %s: %s \n", predictionUser, res);
			
			for (Map.Entry<Long, Double> entry : res.entrySet())
			{
				System.out.println(entry.getKey() + " => " + entry.getValue());
			}
			
			/* Recommendation */
			ItemRecommender irec = rec.getItemRecommender();
			
			ResultList recs = irec.recommendWithDetails(predictionUser, ratingsCount, null, null);
			
			System.out.format("Recommendations for user %d:\n", predictionUser);
			for (Result item : recs) 
			{
				String name = names.getItemName(item.getId());
				System.out.format("\t%d (%s): %.2f\n", item.getId(), name, item.getScore());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
