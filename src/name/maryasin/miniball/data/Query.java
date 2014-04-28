package name.maryasin.miniball.data;

import java.io.*;
import java.util.*;
import android.util.Log;

/**
 * Запрос к списку танцев.
 * Пока представляет собой обёртку вокруг Set<String>.
 * @author mars
 *
 */
public class Query {
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
		aliases = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		aliases.addAll(old.aliases);
		aliases.add(alias);
	}
	/** Сопоставляет псевдонимы танца с запросом.
		Возвращает true, если танец соответствует запросу. */
	public boolean match(Dance dance) {
		return dance.getAliases().containsAll(aliases);
	}
	
	/** Преобразует в массив строк, для сериализации.
	 * (массив, а не одна строка - чтобы избежать проблем со спецсимволами) */
	public String[] serialize() {
		return aliases.toArray(new String[0]);
	}
	public static Query deserialize(String[] src) {
		if(src == null) {
			Log.e("DataManager.Query", "src==null");
			throw new NullPointerException("Ошибка: src не задан");
		}
		Set<String> s = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		s.addAll(Arrays.asList(src));
		return new Query(s);
	}
	
	/** Сравнивает этот запрос с другим */
	@Override public boolean equals(Object o) {
		if(!(o instanceof Query))
			return false;
		return ((Query)o).aliases.equals(aliases);
	}
}
