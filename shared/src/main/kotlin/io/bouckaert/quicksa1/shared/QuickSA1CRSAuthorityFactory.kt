package io.bouckaert.quicksa1.shared

import org.geotools.metadata.iso.citation.Citations
import org.geotools.referencing.ReferencingFactoryFinder
import org.geotools.util.SimpleInternationalString
import org.opengis.metadata.citation.Citation
import org.opengis.referencing.FactoryException
import org.opengis.referencing.IdentifiedObject
import org.opengis.referencing.crs.*
import org.opengis.util.InternationalString

class QuickSA1CRSAuthorityFactory(
    private val crsFactory: CRSFactory = ReferencingFactoryFinder.getCRSFactory(null)
): CRSAuthorityFactory {

    private val wkt = "PROJCS[\"WGS 84 / Pseudo-Mercator\", GEOGCS[\"WGS 84\", DATUM[\"World Geodetic System 1984\", SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], AUTHORITY[\"EPSG\",\"6326\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4326\"]], PROJECTION[\"Popular Visualisation Pseudo Mercator\", AUTHORITY[\"EPSG\",\"1024\"]], PARAMETER[\"semi_minor\", 6378137.0], PARAMETER[\"latitude_of_origin\", 0.0], PARAMETER[\"central_meridian\", 0.0], PARAMETER[\"scale_factor\", 1.0], PARAMETER[\"false_easting\", 0.0], PARAMETER[\"false_northing\", 0.0], UNIT[\"m\", 1.0], AXIS[\"Easting\", EAST], AXIS[\"Northing\", NORTH], AUTHORITY[\"EPSG\",\"3857\"]]"
    override fun getVendor(): Citation = Citations.fromName("Ramon Bouckaert")

    override fun getAuthority(): Citation = Citations.EPSG

    override fun getAuthorityCodes(type: Class<out IdentifiedObject>?): Set<String> = setOf("EPSG:3857")

    override fun getDescriptionText(code: String?): InternationalString =
        SimpleInternationalString("WGS 84 / Pseudo-Mercator")

    override fun createObject(code: String?): IdentifiedObject = createCoordinateReferenceSystem(code)

    override fun createCoordinateReferenceSystem(code: String?): CoordinateReferenceSystem = crsFactory.createFromWKT(wkt)

    override fun createCompoundCRS(code: String?): CompoundCRS {
        throw FactoryException("Not implemented")
    }

    override fun createDerivedCRS(code: String?): DerivedCRS {
        throw FactoryException("Not implemented")
    }

    override fun createEngineeringCRS(code: String?): EngineeringCRS {
        throw FactoryException("Not implemented")
    }

    override fun createGeographicCRS(code: String?): GeographicCRS = createCoordinateReferenceSystem(code) as GeographicCRS

    override fun createGeocentricCRS(code: String?): GeocentricCRS {
        throw FactoryException("Not implemented")
    }

    override fun createImageCRS(code: String?): ImageCRS {
        throw FactoryException("Not implemented")
    }

    override fun createProjectedCRS(code: String?): ProjectedCRS = createGeographicCRS(code) as ProjectedCRS

    override fun createTemporalCRS(code: String?): TemporalCRS {
        throw FactoryException("Not implemented")
    }

    override fun createVerticalCRS(code: String?): VerticalCRS {
        throw FactoryException("Not implemented")
    }
}