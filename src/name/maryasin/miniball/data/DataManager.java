package name.maryasin.miniball.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.Environment;
import android.util.Log;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DataManager {

    /**
     * An array of sample (dummy) items.
     */
//    public static List<Tag> TAGS = new ArrayList<Tag>();

    /**
     * A map of sample (dummy) items, by ID.
     */
//    public static Map<String, Tag> ITEM_MAP = new HashMap<String, Tag>();

    static {
        // Add 3 sample items.
        addItem(new Tag("1", "Item 1"));
        addItem(new Tag("2", "Item 2"));
        addItem(new Tag("3", "Item 3"));
    }

    private static void addItem(Tag item) {
//        TAGS.add(item);
//        ITEM_MAP.put(item.id, item);
    }
    /////////////////////////////////
    
    public static File rootPath = new File(
    		Environment.getExternalStorageDirectory(),
    		"MiniBall"); // TODO: configurable root path

    static {
        try {
			loadDances();
		} catch (IOException e) {
			Log.e("DataManager", "Не удалось загрузить танцы!", e);
		}
    }
    
    public static Map<String, Dance> danceMap;
    public static Set<String> allTags;
    public static List<String> allTagsList;
    public static void loadDances() throws IOException {
    	// TODO: загрузить танцы
    	if(!rootPath.isDirectory())
    		throw new IOException("Root path "+rootPath+" is not a directory!");
    	
    	Log.d("DataManager", "Загружаем танцы");
    	danceMap = new HashMap<String, Dance>();
    	for(File f: rootPath.listFiles())
    		if(f.isDirectory() && // рассматриваем только папки,
    				new File(f, "_.tag").exists()) { // в которых есть общие теги
    			Dance d = new Dance(f.getName());
    			danceMap.put(d.name, d);
    		}

    	Log.d("DataManager", "Загружаем теги");
    	allTags = new HashSet<String>();
    	for(Dance d: danceMap.values()) {
    		d.loadTags(); // отдельной операцией - для возможного отображения прогресса
    		allTags.addAll(d.tags);
    	}
    	allTagsList = new ArrayList<String>();
    	allTagsList.addAll(allTags);
    	Collections.sort(allTagsList);
    	Log.d("DataManager", "Танцы и теги загружены");
    }
    
    private static Map<Query, List<Dance>> danceSearchHash = new HashMap<Query, List<Dance>>();
    public static List<Dance> findDances(Query q) {
    	if(danceSearchHash.containsKey(q)) // если по этому запросу уже есть ответ
    		return danceSearchHash.get(q);
    	List<Dance> ret = new ArrayList<Dance>();
    	for(Dance d: danceMap.values())
    		if(q.match(d))
    			ret.add(d);
    	Collections.sort(ret);
    	danceSearchHash.put(q, ret); // запоминаем результат на будущее
    	return ret;
    }
    
    public static class Tag {
        public String id;
        public String title;

        public Tag(String id, String title) {
            this.id = id;
            this.title = title;
        }
        
        @Override
        public String toString() {
            return title;
        }
    }
    
    /**
     * Запрос к списку танцев.
     * Пока представляет собой обёртку вокруг Set<String>.
     * @author mars
     *
     */
    public static class Query {
    	Set<String> tags;
    	/** Конструирует запрос без тегов, которому соответствует любой танец */
    	public Query() {
    		this.tags = Collections.emptySet();
    	}
    	/** Конструирует запрос из одного тега */
    	public Query(String tag) {
    		this.tags = Collections.singleton(tag);
    	}
    	/** Конструирует запрос из нескольких тегов (AND) */
    	public Query(Set<String> tags) {
    		this.tags = tags;
    	}
    	/** Сопоставляет теги танца с запросом.
			Возвращает true, если танец соответствует запросу. */
    	public boolean match(Dance dance) {
    		return dance.tags.containsAll(tags);
    	}
    	
    	@Override public boolean equals(Object o) {
    		if(!(o instanceof Query))
    			return false;
    		return ((Query)o).tags.equals(tags);
    	}
    }
    
    public static class Dance implements Comparable<Dance> {
    	public String name;
    	private Set<String> tags;
    	
    	public Dance(String name) {
    		this.name = name;
    	}
    	/** Необходимое дополнение к инициализации конструктором! */
    	public void loadTags() throws IOException {
    		// файл должен существовать - проверяли при загрузке списка танцев
    		FileInputStream fin = new FileInputStream(new File(
    				new File(rootPath, name), "_.tag"));
    		try {
        		BufferedReader r = new BufferedReader(new InputStreamReader(fin));
        		try {
					this.tags = new HashSet<String>();
					String line;
					while ((line = r.readLine()) != null)
						this.tags.add(line); // TODO: проверять теги на валидность. Комменты?
				} finally {
					r.close();
				}
    		} finally {
    			fin.close();
    		}
    	}
    	
    	@Override
    	public String toString() {
    		return name; // TODO: доп.информация (теги, например)
    	}
    	/** сравниваем только имя */
		@Override
		public int compareTo(Dance another) {
			return name.compareToIgnoreCase(another.name);
		}
    }
    
    public static class Material {
    	public String dance;
    	public String name;
    	
    	public Material(String dance, String name) {
    		this.dance = dance;
    		this.name = name;
    	}
    	
    	@Override
    	public String toString() {
    		return dance+": "+name;
    	}
    }
}
