def jarjar_action(actions, label, rules, input, output, jarjar):
    actions.run(
        inputs = [rules, input],
        outputs = [output],
        executable = jarjar,
        progress_message = "jarjar %s" % label,
        arguments = ["process", rules.path, input.path, output.path],
    )
    return output

def _jar_jar_impl(ctx):
    jar = jarjar_action(
        actions = ctx.actions,
        label = ctx.label,
        rules = ctx.file.rules,
        input = ctx.file.input_jar,
        output = ctx.outputs.jar,
        jarjar = ctx.executable.jarjar_runner,
    )
    return [
        DefaultInfo(
            files = depset([jar]),
            runfiles = ctx.runfiles(files = [jar]),
        ),
        JavaInfo(
            output_jar = jar,
            compile_jar = jar,
        ),
    ]

jar_jar = rule(
    implementation = _jar_jar_impl,
    attrs = {
        "input_jar": attr.label(allow_single_file = True),
        "rules": attr.label(allow_single_file = True),
        "jarjar_runner": attr.label(
            executable = True,
            cfg = "host",
            default = Label("@bazel_tools//third_party/jarjar:jarjar_bin"),
        ),
    },
    outputs = {
        "jar": "%{name}.jar",
    },
    provides = [JavaInfo],
)
