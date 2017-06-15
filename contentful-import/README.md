## Zillion Tools: Contentful Import

Import tool for Contentful data.

*IMPORTANT:* Make sure `articles_config.json` and `recipes_config.json` both have valid `oauthtoken(mgmtToken)` and `spaceid` values.

Open the project in IntelliJ or any other IDE supporting Java.

#### Step 1

Open `Importer.java` file and add following line in the beginning of main method:

```
args = new String[]{"articles_config.json", "all.xml"};
```

Run `Importer.main()`.

It will import data from `.xml` file and start creating entries in the Contentful and save `entryIds` in `entryHistory.map` file for later use.

#### STEP 2

Remove/comment line mentioned in Step 1 and add following line in the beginnning of main method:

```
args = new String[]{"recipes_config.json", "recipes_v3.tsv"};
```

Run `Importer.main()` again.

First, it will create all recipe's images and store them in `imageMappings.map` file.

Second, it will create all recipes content like (title, nutrition, ingredients etc) and associate each recipe with its respective image asset.

Lastly, it will read previous `entryHistory.map` file, query Contentful and override the `articleDescription` field of each entry which contains `index.php` URLs with the MEMFE urls `/#/viewArticle/recipeContentfulId`.
