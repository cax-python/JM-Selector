package com.cax.select.db;

public interface HistoryRepository {

    // 按 编号 + 偏好 查询历史；不存在返回 null
    AlbumRecord find(String albumId, String preference);

    // 写入/覆盖一条历史, 联合主键：编号 + 偏好
    void upsert(AlbumRecord record);
}
