assert new File(basedir, 'target/classes/test1/model/Test1Table.class').exists();
assert new File(basedir, 'target/generated-sources/entities/test1/model/Test1Table.java').exists();

return true;
