package io.openraven.magpie.core.dmap.service;

import io.openraven.magpie.core.dmap.model.EC2Target;
import io.openraven.magpie.core.dmap.model.VpcConfig;

import java.util.List;
import java.util.Map;

public interface DMapAssetService {
  Map<VpcConfig, List<EC2Target>> groupScanTargets();
}
