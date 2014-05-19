assert new File(basedir, 'target/classes/test2/model/Test2Table.class').exists();
assert new File(basedir, 'target/generated-sources/entities/test2/model/Test2Table.java').exists();

return true;
