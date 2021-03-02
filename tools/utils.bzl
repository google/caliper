def index_artifacts(artifacts):
    artifactMap = {}
    for artifact in artifacts:
        (group_id, artifact_id, version) = artifact.split(":")
        artifactMap["%s:%s" % (group_id, artifact_id)] = version
    return artifactMap
