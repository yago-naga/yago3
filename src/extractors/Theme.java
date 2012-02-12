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
	
	public Theme(String name) {
		this.name=name;
	}
	
	/** Returns the file name of this theme in the given folder*/
	public File file(File folder) {
		return(new File(folder,name+".ttl"));
	}
	
	@Override
	public int compareTo(Theme o) {	
		return name.compareTo(o.name);
	}
	
	@Override
	public boolean equals(Object obj) {	
		return name.equals(obj.toString());
	}
	@Override
	public String toString() {	
		return name;
	}
	@Override
	public int hashCode() {	
		return name.hashCode();
	}
}
