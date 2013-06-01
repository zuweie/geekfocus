package oz.geekfocus.me;

public class Rssbean {
	public String title;
	public String url;
	@Override
	public String toString() {
		if (title != null)
			return title;
		else
			return "I am empty bean!";
	}
	
}
