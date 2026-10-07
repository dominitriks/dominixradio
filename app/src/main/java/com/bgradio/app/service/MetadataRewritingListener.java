package com.bgradio.app.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import java.util.List;

/**
 * Forwards EVERY {@link Player.Listener} callback to {@code delegate}, replacing only the metadata
 * passed to {@link #onMediaMetadataChanged} with {@code player.getMediaMetadata()} (station name as
 * title, current song as subtitle).
 *
 * <p>Written in Java on purpose: Kotlin interface delegation ({@code by delegate}) does not forward
 * Java default methods, which silently dropped all playback state events — the MediaSession then
 * never saw the player playing, so no notification and no foreground service were started.
 */
@OptIn(markerClass = UnstableApi.class)
@SuppressWarnings("deprecation")
final class MetadataRewritingListener implements Player.Listener {

  private final Player player;
  private final Player.Listener delegate;

  MetadataRewritingListener(Player player, Player.Listener delegate) {
    this.player = player;
    this.delegate = delegate;
  }

  @Override public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
    delegate.onMediaMetadataChanged(player.getMediaMetadata());
  }

  @Override public void onEvents(@NonNull Player p, @NonNull Player.Events events) { delegate.onEvents(p, events); }
  @Override public void onTimelineChanged(@NonNull Timeline timeline, int reason) { delegate.onTimelineChanged(timeline, reason); }
  @Override public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) { delegate.onMediaItemTransition(mediaItem, reason); }
  @Override public void onTracksChanged(@NonNull Tracks tracks) { delegate.onTracksChanged(tracks); }
  @Override public void onPlaylistMetadataChanged(@NonNull MediaMetadata mediaMetadata) { delegate.onPlaylistMetadataChanged(mediaMetadata); }
  @Override public void onIsLoadingChanged(boolean isLoading) { delegate.onIsLoadingChanged(isLoading); }
  @Override public void onLoadingChanged(boolean isLoading) { delegate.onLoadingChanged(isLoading); }
  @Override public void onAvailableCommandsChanged(@NonNull Player.Commands commands) { delegate.onAvailableCommandsChanged(commands); }
  @Override public void onTrackSelectionParametersChanged(@NonNull TrackSelectionParameters parameters) { delegate.onTrackSelectionParametersChanged(parameters); }
  @Override public void onPlayerStateChanged(boolean playWhenReady, int playbackState) { delegate.onPlayerStateChanged(playWhenReady, playbackState); }
  @Override public void onPlaybackStateChanged(int playbackState) { delegate.onPlaybackStateChanged(playbackState); }
  @Override public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) { delegate.onPlayWhenReadyChanged(playWhenReady, reason); }
  @Override public void onPlaybackSuppressionReasonChanged(int reason) { delegate.onPlaybackSuppressionReasonChanged(reason); }
  @Override public void onIsPlayingChanged(boolean isPlaying) { delegate.onIsPlayingChanged(isPlaying); }
  @Override public void onRepeatModeChanged(int repeatMode) { delegate.onRepeatModeChanged(repeatMode); }
  @Override public void onShuffleModeEnabledChanged(boolean enabled) { delegate.onShuffleModeEnabledChanged(enabled); }
  @Override public void onPlayerError(@NonNull PlaybackException error) { delegate.onPlayerError(error); }
  @Override public void onPlayerErrorChanged(@Nullable PlaybackException error) { delegate.onPlayerErrorChanged(error); }
  @Override public void onPositionDiscontinuity(int reason) { delegate.onPositionDiscontinuity(reason); }
  @Override public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) { delegate.onPositionDiscontinuity(oldPosition, newPosition, reason); }
  @Override public void onPlaybackParametersChanged(@NonNull PlaybackParameters parameters) { delegate.onPlaybackParametersChanged(parameters); }
  @Override public void onSeekBackIncrementChanged(long ms) { delegate.onSeekBackIncrementChanged(ms); }
  @Override public void onSeekForwardIncrementChanged(long ms) { delegate.onSeekForwardIncrementChanged(ms); }
  @Override public void onMaxSeekToPreviousPositionChanged(long ms) { delegate.onMaxSeekToPreviousPositionChanged(ms); }
  @Override public void onAudioSessionIdChanged(int audioSessionId) { delegate.onAudioSessionIdChanged(audioSessionId); }
  @Override public void onAudioAttributesChanged(@NonNull AudioAttributes attributes) { delegate.onAudioAttributesChanged(attributes); }
  @Override public void onVolumeChanged(float volume) { delegate.onVolumeChanged(volume); }
  @Override public void onSkipSilenceEnabledChanged(boolean enabled) { delegate.onSkipSilenceEnabledChanged(enabled); }
  @Override public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) { delegate.onDeviceInfoChanged(deviceInfo); }
  @Override public void onDeviceVolumeChanged(int volume, boolean muted) { delegate.onDeviceVolumeChanged(volume, muted); }
  @Override public void onVideoSizeChanged(@NonNull VideoSize videoSize) { delegate.onVideoSizeChanged(videoSize); }
  @Override public void onSurfaceSizeChanged(int width, int height) { delegate.onSurfaceSizeChanged(width, height); }
  @Override public void onRenderedFirstFrame() { delegate.onRenderedFirstFrame(); }
  @Override public void onCues(@NonNull List<Cue> cues) { delegate.onCues(cues); }
  @Override public void onCues(@NonNull CueGroup cueGroup) { delegate.onCues(cueGroup); }
  @Override public void onMetadata(@NonNull Metadata metadata) { delegate.onMetadata(metadata); }
}
