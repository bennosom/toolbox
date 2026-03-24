package io.engst.launcher.data.store

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import io.engst.launcher.data.proto.GridDataProto
import java.io.InputStream
import java.io.OutputStream

object GridDataProtoSerializer : Serializer<GridDataProto> {

    override val defaultValue: GridDataProto = GridDataProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): GridDataProto =
        try {
            GridDataProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read GridDataProto", exception)
        }

    override suspend fun writeTo(t: GridDataProto, output: OutputStream) {
        t.writeTo(output)
    }
}
