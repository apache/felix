package test;

public class Child extends Parent {

	Child(int i, String f) {
		super(i, f);
	}

	Child() {
		super(5, "foo");
	}

	public int doSomething() {
		return getIndex() + 1; // 9
	}

}
