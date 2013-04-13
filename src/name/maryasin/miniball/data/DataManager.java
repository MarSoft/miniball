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
	public static Set<String> allAliases;
	public static List<String> allAliasList;

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
					new File(f, "_alias").exists()) { // в которых есть псевдонимы
				Dance d = new Dance(f.getName());
				danceMap.put(d.name, d);
			}

		Log.d("DataManager", "Загружаем псевдонимы");
		allAliases = new HashSet<String>();
		for(Dance d: danceMap.values()) {
			d.loadAliases(); // отдельной операцией - для возможного отображения прогресса
			allAliases.addAll(d.aliases);
		}
		allAliasList = new ArrayList<String>();
		allAliasList.addAll(allAliases);
		Collections.sort(allAliasList);
		Log.d("DataManager", "Танцы и псевдонимы загружены");
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

	/** Суперкласс для Танцев и Псевдонимов */
	protected static abstract class MainItem implements Comparable<MainItem> {
		private String name;
		
		public MainItem(String name) {
			this.name = name;
		}
		
		/** Возвращает название данного элемента */
		public String getName() {
			return name;
		}
		
		@Override
		public int compareTo(MainItem another) {
			return name.compareToIgnoreCase(another.name);
		}

		@Override
		public String toString() {
			return name;
		}
	}
	/** Этот класс представляет псевдоним того или иного танца */
	public class Alias extends MainItem {
		public Alias(String name) {
			super(name);
		}
	}
	/**
	 * Этот класс представляет один танец из базы данных
	 */
	public static class Dance extends MainItem {
		public String name;
		private Set<String> aliases;

		/** После инициализации обязательно вызвать loadAliases() */
		public Dance(String name) {
			super(name);
		}
		/** Необходимое дополнение к инициализации конструктором! */
		public void loadAliases() throws IOException {
			// файл должен существовать - проверяли при загрузке списка танцев
			FileInputStream fin = new FileInputStream(new File(
					new File(rootPath, name), "_alias"));
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(fin));
				try {
					this.aliases = new HashSet<String>();
					String line;
					while ((line = r.readLine()) != null)
						this.aliases.add(line); // TODO: проверять псевдонимы на валидность. Комменты?
				} finally {
					r.close();
				}
			} finally {
				fin.close();
			}
		}
		/**
		 * Возвращает Set псевдонимов танца. Проверяет, загружены ли уже псевдонимы.
		 * @throws IllegalStateException если псевдонимы не были загружены (т.е. танец не инициализирован как положено)
		 * @return
		 */
		public Set<String> getAliases() {
			if(aliases == null) {
				Log.e("DataManager.Dance", "Внимание: запрос к getAliases до загрузки псевдонимов");
				throw new IllegalStateException("Внимание: запрос к getAliases до загрузки псевдонимов");
			}
			return aliases;
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
		Set<String> aliases;
		/** Конструирует запрос без псевдонимов, которому соответствует любой танец */
		public Query() {
			this.aliases = Collections.emptySet();
		}
		/** Конструирует запрос из одного псевдонима */
		public Query(String alias) {
			this.aliases = Collections.singleton(alias);
		}
		/** Конструирует запрос из нескольких псевдонимов (AND) */
		public Query(Set<String> aliases) {
			this.aliases = aliases;
		}
		/** Конструирует запрос, добавляя новый псевдоним к псевдонимам другого запроса */
		public Query(Query old, String alias) {
			aliases = new HashSet<String>();
			aliases.addAll(old.aliases);
			aliases.add(alias);
		}
		/** Сопоставляет псевдонимы танца с запросом.
			Возвращает true, если танец соответствует запросу. */
		public boolean match(Dance dance) {
			return dance.aliases.containsAll(aliases);
		}
		
		/** Сравнивает этот запрос с другим */
		@Override public boolean equals(Object o) {
			if(!(o instanceof Query))
				return false;
			return ((Query)o).aliases.equals(aliases);
		}
	}
}
