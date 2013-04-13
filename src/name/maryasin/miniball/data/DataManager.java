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
	 * Path to root directory of database
	 */
	public static File rootPath = new File(
			Environment.getExternalStorageDirectory(),
			"MiniBall"); // TODO: configurable root path

//	public static List<Dance> ITEMS = new ArrayList<Dance>();

	/**
	 * A map of sample (dummy) items, by ID.
	 */
	public static Map<String, Dance> danceMap = new HashMap<String, Dance>();
	public static Set<String> allTags;
	public static List<String> allTagsList;

	static {
		try {
			initDanceList();
		} catch (IOException e) {
			Log.e("DataManager", "Не удалось загрузить танцы!", e);
		}
	}

	public static void initDanceList() throws IOException {
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

	/**
	 * A dummy item representing a piece of content.
	 */
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
		/**
		 * Возвращает Set тегов танца. Проверяет, загружены ли уже теги.
		 * @throws IllegalStateException если теги не были загружены (т.е. танец не инициализирован как положено)
		 * @return
		 */
		public Set<String> getTags() {
			if(tags == null) {
				Log.e("DataManager.Dance", "Внимание: запрос к getTags до загрузки тегов");
				throw new IllegalStateException("Внимание: запрос к getTags до загрузки тегов");
			}
			return tags;
		}

		@Override
		public String toString() {
			return name;
		}
		/** сравниваем только имя */
		@Override
		public int compareTo(Dance another) {
			return name.compareToIgnoreCase(another.name);
		}
	}
	/**
	 * Материал танца.
	 * TODO: Может включать музыку, текст, html, видео и теги.
	 */
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
}
