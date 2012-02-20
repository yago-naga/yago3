package extractors;

import java.io.File;

/**
 * Represents a theme
 * 
 * @author Fabian M. Suchanek
 *
 */
public class Theme implements Comparable<Theme>{

	/** Name of the theme*/
	public final String name;

	/** Counts the themes*/
	protected static int ids=0;
	
	/** (dynamic) Id of this theme*/
	protected final int id=ids++;
	
	public Theme(String name) {
		this.name=name;
	}
	
	/** Returns the file name of this theme in the given folder*/
	public File file(File folder) {
		return(new File(folder,name+".ttl"));
	}
	
	@Override
	public int compareTo(Theme o) {	
		return id>o.id?1:id<o.id?-1:0;
	}
	
	@Override
	public boolean equals(Object obj) {	
		return (obj instanceof Theme) && ((Theme)obj).id==id;
	}
	@Override
	public String toString() {	
		return name;
	}
	@Override
	public int hashCode() {	
		return id;
	}
}
