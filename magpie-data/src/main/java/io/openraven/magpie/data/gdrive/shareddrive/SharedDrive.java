package io.openraven.magpie.data.gdrive.shareddrive;

import io.openraven.magpie.data.gdrive.GDriveResource;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@Table(name = io.openraven.magpie.data.gdrive.shareddrive.SharedDrive.TABLE_NAME)
public class SharedDrive extends GDriveResource {

  protected static final String TABLE_NAME = "gdriveshareddrive";
  public static final String RESOURCE_TYPE = "GDrive::SharedDrive::SharedDrive";

  public SharedDrive() {
    this.resourceType = RESOURCE_TYPE;
  }
}
