package importers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONObject;

public class TabSeparatedStructure {
	public TabSeparatedStructure readTSVFile(String filename) {
		BufferedReader reader = null;
		
		try { reader = new BufferedReader(new FileReader(filename)); }
		catch(FileNotFoundException e) { e.printStackTrace(); System.exit(1); }

		TabSeparatedStructure tss = new TabSeparatedStructure();

		try {
			String line = reader.readLine();
			tss.fieldHeadings = new ArrayList<String>(Arrays.asList(line.split("	")));

			while((line = reader.readLine()) != null) {
				tss.recipes.add(new Recipe(line));
			}
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return tss;
	}

	public TabSeparatedStructure() {
		recipes = new ArrayList<Recipe>();
	}

	public Map<String, ArrayList<JSONObject>> toJSONObjects() throws Exception{
		Map<String, ArrayList<JSONObject>> jsonObjects = new HashMap<String, ArrayList<JSONObject>>();
		ArrayList<JSONObject> objects = new ArrayList<JSONObject>();

		for(Recipe recipe : recipes) {
			JSONObject json = new JSONObject();

			for(int i = 0; i < fieldHeadings.size(); i++) {
				String field = recipe.fields.get(i);
				try {
					float fieldFloat = Float.parseFloat(field);
					json.put(fieldHeadings.get(i), fieldFloat);
				}
				catch(NumberFormatException e) {
					json.put(fieldHeadings.get(i), field);
				}
			}

			objects.add(json);
		}

		jsonObjects.put("Content", objects);

		ArrayList<JSONObject> images = new ArrayList<JSONObject>();
		for(JSONObject object : objects) {
			JSONObject image = new JSONObject();
			String src = object.getString("image").trim();
			if(src.length() == 0) continue;

			image.put("src", src);
			images.add(image);
		}

		jsonObjects.put("Image", images);

		return jsonObjects;
	}

	public class Recipe {
		public Recipe(String line) {
			fields = new ArrayList<String>(Arrays.asList(line.split("	")));
			if(fields.size() != 28) {
				for(String field : fields) {
					System.out.println(field);
				}
			}
		}
		public ArrayList<String> fields;
	}

	ArrayList<String> fieldHeadings;
	ArrayList<Recipe> recipes;
}