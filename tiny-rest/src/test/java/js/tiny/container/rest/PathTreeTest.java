package js.tiny.container.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import js.tiny.container.rest.PathTree.Item;

public class PathTreeTest {
	private PathTree<Object> tree;

	@Before
	public void beforeTest() {
		tree = new PathTree<>();
	}

	@Test
	public void GivenSingleComponentPath_WhenGet_ThenFoundValueAndVariablesEmpty() {
		// given
		Object value = new Object();
		tree.put(paths("book"), value);

		// when
		Item<Object> item = tree.get(paths("book"));

		// then
		assertThat(item, notNullValue());
		assertThat(item.getValue(), equalTo(value));
		assertThat(item.getVariables(), is(empty()));
	}

	@Test
	public void GivenSingleComponentPath_WhenGetMisspelled_ThenNullValueAndVariablesEmpty() {
		// given
		Object value = new Object();
		tree.put(paths("book"), value);

		// when
		Item<Object> item = tree.get(paths("bookx"));

		// then
		assertThat(item, notNullValue());
		assertThat(item.getValue(), nullValue());
		assertThat(item.getVariables(), is(empty()));
	}

	@Test
	public void GivenTwoComponentPaths_WhenGet_ThenFoundValueAndVariablesEmpty() {
		// given
		Object value = new Object();
		tree.put(paths("author", "book"), value);

		// when
		Item<Object> item = tree.get(paths("author", "book"));

		// then
		assertThat(item, notNullValue());
		assertThat(item.getValue(), equalTo(value));
		assertThat(item.getVariables(), is(empty()));
	}

	@Test
	public void GivenSingleVariable_WhenGet_ThenFoundValueAndLoadVariable() {
		// given
		Object value = new Object();
		tree.put(paths("book", "{isbn}"), value);

		// when
		Item<Object> item = tree.get(paths("book", "978-973-46-3185-8"));

		// then
		assertThat(item, notNullValue());
		assertThat(item.getValue(), equalTo(value));
		assertThat(item.getVariables(), contains("978-973-46-3185-8"));
	}

	@Test
	public void GivenTwoVariables_WhenGet_ThenFoundValueAndLoadVariables() {
		// given
		Object value = new Object();
		tree.put(paths("book", "{store}", "{isbn}"), value);

		// when

		// then
		Item<Object> item = tree.get(paths("book", "libris", "978-973-46-3185-8"));
		assertThat(item, notNullValue());
		assertThat(item.getValue(), equalTo(value));
		assertThat(item.getVariables(), contains("libris", "978-973-46-3185-8"));
	}

	@Test
	public void GivenMixedPathComponentsAndVariables_WhenGet_ThenFoundValueAndLoadVariables() {
		// given
		Object value = new Object();
		tree.put(paths("store", "{store}", "book", "{isbn}"), value);

		// when
		Item<Object> item = tree.get(paths("store", "libris", "book", "978-973-46-3185-8"));

		// then
		assertThat(item, notNullValue());
		assertThat(item.getValue(), equalTo(value));
		assertThat(item.getVariables(), contains("libris", "978-973-46-3185-8"));
	}

	@Test
	public void GivenMoreRequestPathComponents_WhenGet_ThenNullValueAndEmptyVariables() {
		// given
		Object value = new Object();
		tree.put(paths("book"), value);

		// when
		Item<Object> item = tree.get(paths("book", "libris"));

		// then
		assertThat(item, notNullValue());
		assertThat(item.getValue(), nullValue());
		assertThat(item.getVariables(), is(empty()));
	}

	@Test
	public void GivenVariableAndMoreRequestPathComponents_WhenGet_ThenNullValueAndVariableLoaded() {
		// given
		Object value = new Object();
		tree.put(paths("book", "{isbn}"), value);

		// when
		Item<Object> item = tree.get(paths("book", "978-973-46-3185-8", "libris"));

		// then
		assertThat(item, notNullValue());
		assertThat(item.getValue(), nullValue());
		assertThat(item.getVariables(), contains("978-973-46-3185-8"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenEmptyPath_WhenPut_ThenException() {
		// given

		// when
		tree.put(paths(), null);

		// then
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

		// then
	}

	private static List<String> paths(String... paths) {
		return Arrays.asList(paths);
	}
}
