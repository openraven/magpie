package io.openraven.magpie.data.gdrive.drive;

import io.openraven.magpie.data.gdrive.GDriveResource;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@Table(name = io.openraven.magpie.data.gdrive.drive.Drive.TABLE_NAME)
public class Drive extends GDriveResource {

  protected static final String TABLE_NAME = "gdrivedrive";
  public static final String RESOURCE_TYPE = "GDrive::Drive::Drive";

  public Drive() {
    this.resourceType = RESOURCE_TYPE;
  }
}

