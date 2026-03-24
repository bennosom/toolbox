package io.engst.launcher.data.store

import io.engst.launcher.data.proto.GridCellProto
import io.engst.launcher.data.proto.GridDataProto
import io.engst.launcher.data.proto.GridPageProto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GridDataProtoSerializerTest {

    private val serializer = GridDataProtoSerializer

    @Test
    fun defaultValue_is_proto_default_instance() {
        assertEquals(GridDataProto.getDefaultInstance(), serializer.defaultValue)
    }

    @Test
    fun round_trip_through_serializer_preserves_data() = runTest {
        val proto = GridDataProto.newBuilder()
            .setCols(3)
            .setRows(5)
            .setPopulated(true)
            .addBar("pkg.X/cls.X")
            .addGrid(
                GridPageProto.newBuilder()
                    .addCells(
                        GridCellProto.newBuilder()
                            .setCol(0).setRow(0).setAppId("pkg.A/cls.A")
                    )
            )
            .build()

        val output = ByteArrayOutputStream()
        serializer.writeTo(proto, output)

        val input = ByteArrayInputStream(output.toByteArray())
        val restored = serializer.readFrom(input)

        assertEquals(proto, restored)
    }

    @Test
    fun empty_stream_reads_as_default() = runTest {
        val input = ByteArrayInputStream(ByteArray(0))
        val result = serializer.readFrom(input)
        assertEquals(GridDataProto.getDefaultInstance(), result)
    }

    @Test
    fun corrupt_stream_throws_corruption_exception() = runTest {
        val input = ByteArrayInputStream(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x01))
        val result = runCatching { serializer.readFrom(input) }
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull() is androidx.datastore.core.CorruptionException
        )
    }
}
