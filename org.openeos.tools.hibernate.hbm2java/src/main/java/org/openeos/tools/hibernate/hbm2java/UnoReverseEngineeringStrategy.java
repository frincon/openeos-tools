/**
 * Copyright 2014 Fernando Rincon Martin <frm.rincon@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openeos.tools.hibernate.hbm2java;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.reveng.AssociationInfo;
import org.hibernate.cfg.reveng.DefaulAssociationInfo;
import org.hibernate.cfg.reveng.DelegatingReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.ReverseEngineeringRuntimeInfo;
import org.hibernate.cfg.reveng.ReverseEngineeringSettings;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.TableIdentifier;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Table;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.hbm2x.StringUtils;

public class UnoReverseEngineeringStrategy extends DelegatingReverseEngineeringStrategy {

	public UnoReverseEngineeringStrategy(ReverseEngineeringStrategy delegate) {
		super(delegate);
		// TODO Auto-generated constructor stub
	}

	private Connection connection;
	private ConnectionProvider connectionProvider;
	private ReverseEngineeringSettings settings;
	private ReverseEngineeringRuntimeInfo runtimeInfo;

	@Override
	public void setSettings(ReverseEngineeringSettings settings) {
		this.settings = settings;
	}

	@Override
	public void configure(ReverseEngineeringRuntimeInfo runtimeInfo) {
		// Get connection from connection provider
		this.connectionProvider = runtimeInfo.getConnectionProvider();
		try {
			this.connection = connectionProvider.getConnection();
		} catch (SQLException e) {
			throw new RuntimeException("A SQLException occured when configure", e);
		}
		this.runtimeInfo = runtimeInfo;
	}

	@Override
	public void close() {
		//Close the connection
		try {
			connectionProvider.closeConnection(connection);
		} catch (SQLException e) {
			throw new RuntimeException("A SQLException occured when close", e);
		}
	}

	@Override
	public String getTableIdentifierStrategyName(TableIdentifier tableIdentifier) {
		return "uuid";
	}

	@Override
	public boolean isForeignKeyCollectionInverse(String name, TableIdentifier foreignKeyTable, List columns,
			TableIdentifier foreignKeyReferencedTable, List referencedColumns) {

		Table fkTable = runtimeInfo.getTable(foreignKeyTable);
		if (fkTable == null) {
			return true; // we don't know better
		}

		if (isManyToManyTable(fkTable)) {
			// if the reference column is the first one then we are inverse.
			Column column = fkTable.getColumn(0);
			Column fkColumn = (Column) referencedColumns.get(0);
			if (fkColumn.equals(column)) {
				return true;
			} else {
				return false;
			}
		}
		return true;

	}

	@Override
	public String columnToPropertyName(TableIdentifier table, String column) {

		if (runtimeInfo.getTable(table).getPrimaryKey().getColumns().size() == 1
				&& runtimeInfo.getTable(table).getPrimaryKey().getColumn(0).getName().equals(column)) {
			return "id";
		} else if (column.substring(0, 2).equalsIgnoreCase("is")) {
			return super.columnToPropertyName(table, column);
		} else {
			return super.columnToPropertyName(table, column);
		}
	}

	@Override
	public boolean excludeForeignKeyAsCollection(String keyname, TableIdentifier fromTable, List fromColumns,
			TableIdentifier referencedTable, List referencedColumns) {
		if (referencedTable.getName().equalsIgnoreCase("AD_ORG")) {
			return true;
		}
		return super.excludeForeignKeyAsCollection(keyname, fromTable, fromColumns, referencedTable, referencedColumns);
	}

	@Override
	public String foreignKeyToCollectionName(String keyname, TableIdentifier fromTable, List fromColumns,
			TableIdentifier referencedTable, List referencedColumns, boolean uniqueReference) {
		// TODO Auto-generated method stub
		return super.foreignKeyToCollectionName(keyname, fromTable, fromColumns, referencedTable, referencedColumns,
				uniqueReference);
	}

	@Override
	public String foreignKeyToEntityName(String keyname, TableIdentifier fromTable, List fromColumnNames,
			TableIdentifier referencedTable, List referencedColumnNames, boolean uniqueReference) {
		// TODO Auto-generated method stub
		return super.foreignKeyToEntityName(keyname, fromTable, fromColumnNames, referencedTable, referencedColumnNames,
				uniqueReference);
	}

	@Override
	public boolean excludeForeignKeyAsManytoOne(String keyname, TableIdentifier fromTable, List fromColumns,
			TableIdentifier referencedTable, List referencedColumns) {
		// TODO Auto-generated method stub
		return super.excludeForeignKeyAsManytoOne(keyname, fromTable, fromColumns, referencedTable, referencedColumns);
	}

	@Override
	public String foreignKeyToManyToManyName(ForeignKey fromKey, TableIdentifier middleTable, ForeignKey toKey,
			boolean uniqueReference) {
		// TODO Auto-generated method stub
		return super.foreignKeyToManyToManyName(fromKey, middleTable, toKey, uniqueReference);
	}

	@Override
	public AssociationInfo foreignKeyToAssociationInfo(ForeignKey foreignKey) {
		// TODO Auto-generated method stub
		return super.foreignKeyToAssociationInfo(foreignKey);
	}

	@Override
	public AssociationInfo foreignKeyToInverseAssociationInfo(ForeignKey foreignKey) {
		// TODO Auto-generated method stub
		return super.foreignKeyToInverseAssociationInfo(foreignKey);
	}

	@Override
	public String foreignKeyToInverseEntityName(String keyname, TableIdentifier fromTable, List fromColumnNames,
			TableIdentifier referencedTable, List referencedColumnNames, boolean uniqueReference) {
		// TODO Auto-generated method stub
		return super.foreignKeyToInverseEntityName(keyname, fromTable, fromColumnNames, referencedTable, referencedColumnNames,
				uniqueReference);
	}

	@Override
	public boolean excludePackage(String packageName) {
		return !settings.getDefaultPackageName().equals(packageName);
	}

	@Override
	public boolean isOneToOne(ForeignKey foreignKey) {
		Map tableMap = this.tableToMetaAttributes(TableIdentifier.create(foreignKey.getTable()));
		if (tableMap != null) {
			MetaAttribute forceOneToOne = (MetaAttribute) tableMap.get("force-one-to-one");
			if (forceOneToOne != null) {
				String[] fks = forceOneToOne.getValue().split(",");
				for (int i = 0; i < fks.length; i++) {
					if (fks[i].trim().equalsIgnoreCase(foreignKey.getName())) {
						return true;
					}
				}
			}
		}
		return super.isOneToOne(foreignKey);

	}

	@Override
	public boolean useColumnForOptimisticLock(TableIdentifier identifier, String column) {
		Map columnMeta = this.columnToMetaAttributes(identifier, column);
		if(columnMeta!=null) {
			MetaAttribute isOptimisticAttribute = (MetaAttribute) columnMeta.get("is-optimistic-lock-column");
			if(isOptimisticAttribute!=null) {
				return Boolean.parseBoolean(isOptimisticAttribute.getValue());
			}
		}
		return super.useColumnForOptimisticLock(identifier, column);
	}

}
