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

	/** Карта, соотносящая все Танцы с их именами */
	public static Map<String, Dance> danceMap;
	/** Карта, соотносящая имена не только с Танцами, но и с Псевдонимами */
	public static Map<String, AliasOrDance> aliasMap;

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
		
		try {
			Log.d("DataManager", "Загружаем танцы");
			danceMap = new HashMap<String, Dance>();
			aliasMap = new HashMap<String, AliasOrDance>();
			for(File f: rootPath.listFiles())
				if(f.isDirectory() && // рассматриваем только папки,
						new File(f, "_alias").exists()) { // в которых есть псевдонимы
					Dance d = new Dance(f.getName());
					danceMap.put(d.name, d);
					aliasMap.put(d.name, d);
				}

			Log.d("DataManager", "Загружаем псевдонимы");
			Set<String> aliases = new HashSet<String>();
			// Грузим псевдонимы для всех танцев, запоминаем их перечень
			for(Dance d: danceMap.values()) {
				d.loadAliases(); // отдельной операцией - для возможного отображения прогресса
				aliasMap.put(d.getName(), d);
				aliases.addAll(d.aliases);
			}
			// Создаём уникальные объекты для всех псевдонимов
			for(String a: aliases)
				aliasMap.put(a, new AliasOrDance(a));
			// А теперь отдельным циклом расставляем refCount для всех объектов
			for(Dance d: danceMap.values())
				for(String a: d.getAliases())
					aliasMap.get(a).addRef();
			Log.d("DataManager", "Танцы и псевдонимы загружены");
		} catch (IOException e) {
			// заметаем следы, чтобы не было лишних exceptions при отображении чего попало
			// TODO: может, лучше работать с тем, что удалось загрузить? Возможно, загружать всё что только удастся?
			danceMap = Collections.emptyMap();
			aliasMap = Collections.emptyMap();
			throw e; // TODO надо поймать где-то и вывести сообщение об ошибке
		}
	}

	private static Map<Query, Set<Dance>> danceSearchCache = new HashMap<Query, Set<Dance>>();
	/** Возвращает перечень танцев, соответствующих запросу */
	public static Set<Dance> findDances(Query q) {
		if(danceSearchCache.containsKey(q)) // если по этому запросу уже есть ответ
			return danceSearchCache.get(q);
		Set<Dance> ret = new HashSet<Dance>();
		for(Dance d: danceMap.values())
			if(q.match(d))
				ret.add(d);
		danceSearchCache.put(q, ret); // запоминаем результат на будущее
		return ret;
	}
	private static Map<Query, List<AliasOrDance>> aliasSearchCache = new HashMap<Query, List<AliasOrDance>>();
	/** Возвращает отсортированный список Танцев, соответствующих запросу,
		состоящему из 0 или более имён псевдонимов,
		и Псевдонимов, которые есть у этих танцев */
	public static List<AliasOrDance> findAliases(Query q) {
		if(aliasSearchCache.containsKey(q)) // если по этому запросу уже есть ответ
			return aliasSearchCache.get(q);
		Set<Dance> dset = findDances(q);
		Set<AliasOrDance> aset = new HashSet<AliasOrDance>();
		aset.addAll(dset); // сначала добавим танцы
		for(Dance d: dset)
			for(String a: d.getAliases())
				aset.add(aliasMap.get(a)); // берём из map, чтобы сохранить refcount
		List<AliasOrDance> ret = new ArrayList<AliasOrDance>();
		ret.addAll(aset);
		Collections.sort(ret);
		aliasSearchCache.put(q, ret); // запоминаем результат на будущее
		return ret;
	}

	/** Этот класс представляет псевдоним танца.
		Субклассом его является собственно танец.
		Использоваться данный класс должен только в общем перечне
		псевдонимов и танцев,
		в остальных случаях псевдоним представлен строкой.
	 */
	protected static class AliasOrDance implements Comparable<AliasOrDance> {
		private String name;
		/** Число ссылок. Для псевдонима изначально 0, для танца 1 (он сам). */
		protected int refCount;
		
		public AliasOrDance(String name) {
			this.name = name;
			refCount = 0; // изначально псевдоним ни на что не ссылается
		}
		
		/** Возвращает название данного элемента */
		public String getName() {
			return name;
		}

		/** Вызывается только при загрузке списка танцев и псевдонимов */
		/*package-local*/ void addRef() {
			refCount++;
		}
		/** Возвращает число танцев, использующих этот псевдоним */
		public int getRefCount() {
			return refCount;
		}
		/**
		 * Возвращает все танцы, использующие этот псевдоним.
		 */
		public Set<Dance> getReferringDances() {
			// TODO
			// TODO: кэширование?
			return null;
		}
		/** Возвращает танец (единственный), ссылающийся на этот псевдоним.
		 * Если таких танцев несколько, вызывает IllegalStateError */
		public Dance getReferringDance() {
			if(refCount > 1)
				throw new IllegalStateException("На псевдоним "+getName()+" слишком много сслыок: "+refCount);
			// TODO
			return null;
		}
		
		@Override
		public int compareTo(AliasOrDance another) {
			return name.compareToIgnoreCase(another.name);
		}

		@Override
		public String toString() {
			return name;
		}
	}
	/**
	 * Этот класс представляет один танец из базы данных
	 */
	public static class Dance extends AliasOrDance {
		public String name;
		private Set<String> aliases;

		/** После инициализации обязательно вызвать loadAliases() */
		public Dance(String name) {
			super(name);
			refCount = 1; // любой танец ссылается сам на себя, как минимум
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
		
		/**
		 * Любой танец как псевдоним ссылается как минимум сам на себя.
		 */
		@Override
		public Dance getReferringDance() {
			if(getRefCount() > 1)
				throw new IllegalStateException("На танец "+getName()+" слишком много ссылок: "+getRefCount());
			return this;
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
