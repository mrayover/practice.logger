CREATE TRIGGER block_media_type_insert BEFORE INSERT ON block_media_links WHEN NOT EXISTS (
SELECT 1 FROM media_assets WHERE id=NEW.mediaId AND ((NEW.role='VISUAL' AND category IN ('IMAGE','PDF')) OR (NEW.role='AUDIO' AND category='AUDIO')))
BEGIN SELECT RAISE(ABORT,'Incompatible Block media role'); END;
-- next
CREATE TRIGGER block_media_type_update BEFORE UPDATE ON block_media_links WHEN NOT EXISTS (
SELECT 1 FROM media_assets WHERE id=NEW.mediaId AND ((NEW.role='VISUAL' AND category IN ('IMAGE','PDF')) OR (NEW.role='AUDIO' AND category='AUDIO')))
BEGIN SELECT RAISE(ABORT,'Incompatible Block media role'); END;
-- next
CREATE TRIGGER linked_media_type_update BEFORE UPDATE OF category ON media_assets WHEN EXISTS (
SELECT 1 FROM block_media_links WHERE mediaId=OLD.id AND NOT ((role='VISUAL' AND NEW.category IN ('IMAGE','PDF')) OR (role='AUDIO' AND NEW.category='AUDIO')))
BEGIN SELECT RAISE(ABORT,'Media type is linked to a Block'); END;
-- next
CREATE TRIGGER retire_block_master AFTER DELETE ON media_markers BEGIN
DELETE FROM block_audio_markers WHERE mediaId=OLD.mediaId AND id=OLD.id AND originalMaster='hidden';
UPDATE block_audio_markers SET originalMaster=NULL,hidden=0 WHERE mediaId=OLD.mediaId AND id=OLD.id AND originalMaster IS NOT NULL;
END;
-- next
CREATE TRIGGER practice_overlay_insert BEFORE INSERT ON practice_overlays WHEN
length(trim(NEW.id)) NOT BETWEEN 1 AND 64 OR typeof(NEW.page)!='integer' OR NEW.page NOT BETWEEN 0 AND 99 OR
typeof(NEW.drawOrder)!='integer' OR NEW.drawOrder NOT BETWEEN 0 AND 1000000000 OR
typeof(NEW.appearance)!='integer' OR NOT ((NEW.type='HIGHLIGHT' AND NEW.appearance BETWEEN 0 AND 2) OR (NEW.type IN ('COVER','FOCUS') AND NEW.appearance=0)) OR
NOT (NEW.left>=0 AND NEW.left<NEW.right AND NEW.right<=1 AND NEW.top>=0 AND NEW.top<NEW.bottom AND NEW.bottom<=1) OR NOT (
EXISTS (SELECT 1 FROM block_media_links link JOIN media_assets asset ON asset.id=link.mediaId WHERE link.blockId=NEW.blockId AND link.role='VISUAL' AND NEW.reference='library:'||asset.id AND ((asset.category='IMAGE' AND NEW.page=0) OR (asset.category='PDF' AND NEW.page<asset.pageCount))) OR
EXISTS (SELECT 1 FROM block_images image WHERE image.blockId=NEW.blockId AND NEW.reference='legacy:'||image.fileName AND (image.mimeType='application/pdf' OR NEW.page=0) AND NOT EXISTS (SELECT 1 FROM block_media_links WHERE blockId=NEW.blockId AND role='VISUAL')))
BEGIN SELECT RAISE(ABORT,'Invalid practice overlay'); END;
-- next
CREATE TRIGGER practice_overlay_update BEFORE UPDATE ON practice_overlays WHEN
length(trim(NEW.id)) NOT BETWEEN 1 AND 64 OR typeof(NEW.page)!='integer' OR NEW.page NOT BETWEEN 0 AND 99 OR
typeof(NEW.drawOrder)!='integer' OR NEW.drawOrder NOT BETWEEN 0 AND 1000000000 OR
typeof(NEW.appearance)!='integer' OR NOT ((NEW.type='HIGHLIGHT' AND NEW.appearance BETWEEN 0 AND 2) OR (NEW.type IN ('COVER','FOCUS') AND NEW.appearance=0)) OR
NOT (NEW.left>=0 AND NEW.left<NEW.right AND NEW.right<=1 AND NEW.top>=0 AND NEW.top<NEW.bottom AND NEW.bottom<=1) OR NOT (
EXISTS (SELECT 1 FROM block_media_links link JOIN media_assets asset ON asset.id=link.mediaId WHERE link.blockId=NEW.blockId AND link.role='VISUAL' AND NEW.reference='library:'||asset.id AND ((asset.category='IMAGE' AND NEW.page=0) OR (asset.category='PDF' AND NEW.page<asset.pageCount))) OR
EXISTS (SELECT 1 FROM block_images image WHERE image.blockId=NEW.blockId AND NEW.reference='legacy:'||image.fileName AND (image.mimeType='application/pdf' OR NEW.page=0) AND NOT EXISTS (SELECT 1 FROM block_media_links WHERE blockId=NEW.blockId AND role='VISUAL')))
BEGIN SELECT RAISE(ABORT,'Invalid practice overlay'); END;
-- next
CREATE TRIGGER practice_overlay_library_delete BEFORE DELETE ON block_media_links WHEN OLD.role='VISUAL' AND EXISTS (
SELECT 1 FROM practice_overlays WHERE blockId=OLD.blockId AND reference='library:'||OLD.mediaId)
BEGIN SELECT RAISE(ABORT,'Discard markings before changing the visual reference'); END;
-- next
CREATE TRIGGER practice_overlay_library_update BEFORE UPDATE OF blockId,role,mediaId ON block_media_links WHEN OLD.role='VISUAL' AND EXISTS (
SELECT 1 FROM practice_overlays WHERE blockId=OLD.blockId AND reference='library:'||OLD.mediaId) AND (NEW.blockId!=OLD.blockId OR NEW.role!=OLD.role OR NEW.mediaId!=OLD.mediaId)
BEGIN SELECT RAISE(ABORT,'Discard markings before changing the visual reference'); END;
-- next
CREATE TRIGGER practice_overlay_legacy_delete BEFORE DELETE ON block_images WHEN EXISTS (
SELECT 1 FROM practice_overlays WHERE blockId=OLD.blockId AND reference='legacy:'||OLD.fileName)
BEGIN SELECT RAISE(ABORT,'Discard markings before changing the visual reference'); END;
-- next
CREATE TRIGGER practice_overlay_legacy_update BEFORE UPDATE OF blockId,fileName,mimeType ON block_images WHEN EXISTS (
SELECT 1 FROM practice_overlays WHERE blockId=OLD.blockId AND reference='legacy:'||OLD.fileName) AND (NEW.blockId!=OLD.blockId OR NEW.fileName!=OLD.fileName OR NEW.mimeType!=OLD.mimeType)
BEGIN SELECT RAISE(ABORT,'Discard markings before changing the visual reference'); END;
-- next
CREATE TRIGGER practice_overlay_legacy_replacement BEFORE INSERT ON block_media_links WHEN NEW.role='VISUAL' AND EXISTS (
SELECT 1 FROM practice_overlays WHERE blockId=NEW.blockId AND reference LIKE 'legacy:%')
BEGIN SELECT RAISE(ABORT,'Discard markings before changing the visual reference'); END;
