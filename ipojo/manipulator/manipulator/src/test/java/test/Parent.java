package test;

public class Parent {

	private int m_index = 0;

	public Parent(int i, String s) {
		m_index = i + s.length();
	}

	public int getIndex() {
		return m_index;
	}

}
