package test;

public class PojoWithInner {

	private MyInner m_result = new MyInner();

	// This is a simple POJO

	public boolean doSomething() {
		return m_result.getInner();
	}

	public class MyInner {

		public boolean getInner() {
			return true;
		}

	}

}
