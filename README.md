javelin_p1
===
This ORM is to be used with PostgreSQL. It provides an abstraction for JDBC using
class extension, similar to ActiveJDBC, jOOQ, and JavaLite. This project was created
as a part of training with Revature.

Created by Nate Gamble

<br>
How to use
===
- Include the dependency in your project

```
    <dependency>
      <groupId>com.revature</groupId>
      <artifactId>javelin_p1</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
```

- Create a class for each pojo or table in database that you want to use. This class should extend `com.revature.javelin.Model`
    - This class should be empty apart from an optional `@Table` annotation before the class declaration providing the name of the table for the class.
- Create an instance of your class and call methods on it to perform CRUD operations on your database.
- CRUD methods (listed below) are either starting, intermediary and/or terminal. A starting needs to be the first method called when executing a SQL call to the database. Intermediary operations are optional and must come between starting operations and terminal operations. Terminal operations must come at the end of a call. Terminal operations will enact the built SQL call upon the database.

| Name of method    | Type of operation  |
|-------------------|--------------------|
| `create`          | Starting, Terminal |
| `findAll`         | Starting           |
| `findAllById`     | Starting           |
| `findAllByColumn` | Starting           |
| `findColumns`     | Starting           |
| `where`           | Intermediary       |
| `whereAnd`        | Intermediary       |
| `joinUsing`       | Intermediary       |
| `joinOn`          | Intermediary       |
| `update`          | Starting, Terminal |
| `delete`          | Starting           |
| `execute`         | Terminal           |

<br>

- Operations that are both Starting and Terminal will use data currently set in the object for their respective operation (`create` will create a new record in the database with the list of fields and values given so far)
- Use `setColumn` and `changeColumn` to add or modify field/value pairs currently stored in the object
    - `setColumn` and `changeColumn` are functionally similar, but `setColumn` includes some type safety. `setColumn` will throw an exception and not change the value of the column if there is a value already associated with given key and the type of that value and the provided value are different. `changeColumn` ignores the type of the value and changes it regardless.
- Use `get` to retrieve values currently stored in the object. If you want to get values from the database, use a find method paired with `execute`
- `execute` will return a list of objects of the class you provide it. If the SQL statement is not a query (create, update, or delete), the returned list will be empty. If the SQL statement is a query, returns all records found by the query in a list. If the list only contains one object, the `execute` method changes the values of the current object to those of the object returned from the `SELECT` call.