package name.maryasin.miniball.data;

import java.io.*;
import java.util.*;

import android.util.Log;

/** Этот класс представляет псевдоним танца или танец (который является его субклассом).
	Использоваться данный класс должен только в общем перечне
	псевдонимов и танцев,
	в остальных случаях псевдоним представлен строкой.
 */
public class Alias implements Comparable<Alias> {
	private String name;
	/** Число ссылок. Для псевдонима изначально 0, для танца 1 (он сам). */
	protected int refCount;
	private Dance ref; // TODO: пока поддерживает только один ref. А надо ли больше?
	
	/*package*/ Alias(String name) {
		this.name = name;
		refCount = 0; // изначально псевдоним ни на что не ссылается
	}
	/** Конструктор для псевдонима с возможностью указать один ref */
	/*package*/ Alias(String name, Dance d) {
		this(name);
		addRef(d);
	}
	
	/** Возвращает название данного элемента */
	public String getName() {
		return name;
	}

	/** Вызывается только при загрузке списка танцев и псевдонимов.
	 * Добавляет информацию о том, что на это псевдоним ссылается какой-либо танец.
	 * Танец пока не сохраняется, передаём для проформы.
	 * TODO: Возможно, сохранять для оптимизации getReferringDances()? */
	/*package-local*/ void addRef(Dance d) {
		refCount++;
		if(ref == null)
			ref = d;
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
		return ref;
	}
	
	@Override
	public int compareTo(Alias another) {
		return name.compareToIgnoreCase(another.name);
	}
	@Override
	public boolean equals(Object o) {
		return (o instanceof Alias) &&
				((Alias)o).name.equalsIgnoreCase(name);
	}
	/** Танцы/псевдонимы считаются одинаковыми, если совпадает имя. */
	@Override
	public int hashCode() {
		return name.toLowerCase(Locale.getDefault()).hashCode();
		// toLowerCase - во избежание косяков с регистром, когда два танца равны по compareTo, но имеют разные хэши
	}

	@Override
	public String toString() {
		return name;
	}
}
