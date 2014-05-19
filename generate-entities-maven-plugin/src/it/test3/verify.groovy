assert new File(basedir, 'target/classes/test3/model/Test1Table.class').exists();
assert new File(basedir, 'target/generated-sources/entities/test3/model/Test2Table.java').exists();
assert new File(basedir, 'target/classes/test3/model/Test2Table.class').exists();
assert new File(basedir, 'target/generated-sources/entities/test3/model/Test2Table.java').exists();

return true;
