#!/usr/local/bin/bash

ARCH='amd64'

for it in REPOSITORY_OWNER REPOSITORY_NAME SOURCE_COMMIT VCS_PAT ARCH; do
 if test -z "${!it}"; then echo "Argument \"${it}\" is empty!"; exit 1; fi; done

GITHUB_USER="$(curl -f 'https://api.github.com/user' -H "Authorization: token $VCS_PAT")"
if test $? -ne 0; then echo 'Get user error!'; exit 1
elif test -z "$GITHUB_USER"; then echo 'User is empty!'; exit 1; fi

USER_NAME="$(echo "$GITHUB_USER" | yq -erM .name)" || exit 1
USER_ID="$(echo "$GITHUB_USER" | yq -erM .id)" || exit 1
USER_LOGIN="$(echo "$GITHUB_USER" | yq -erM .login)" || exit 1
USER_EMAIL="${USER_ID}+${USER_LOGIN}@users.noreply.github.com"

VARIANT='unstable'
TARGET_BRANCH="${VARIANT}"

HOST='docker.io'
NAMESPACE='kepocnhh'
REPOSITORY="gradle-${ARCH}"
TAG='8.10.2'
IMAGE_NAME="${HOST}/${NAMESPACE}/${REPOSITORY}:${TAG}"

CONTAINER_NAME="${REPOSITORY_OWNER}-${REPOSITORY_NAME}-${VARIANT}"

docker stop "${CONTAINER_NAME}"
docker rm -f "${CONTAINER_NAME}"

docker run --platform="linux/${ARCH}" -id --name "${CONTAINER_NAME}" "${IMAGE_NAME}"

if test $? -ne 0; then echo 'Run error!'; exit 1; fi

WORK_DIR="/${REPOSITORY_OWNER}/${REPOSITORY_NAME}"

docker exec "${CONTAINER_NAME}" mkdir -p "${WORK_DIR}"

if test $? -ne 0; then echo 'Make dir error!'; exit 1; fi

for it in \
 'git init' \
 "git remote add origin https://${VCS_PAT}@github.com/${REPOSITORY_OWNER}/${REPOSITORY_NAME}.git" \
 "git fetch origin ${TARGET_BRANCH}" \
 "git fetch origin ${SOURCE_COMMIT}" \
 "git checkout ${TARGET_BRANCH}"; do
 docker exec -w "${WORK_DIR}" "${CONTAINER_NAME}" /usr/local/bin/bash -c "${it}"
 if test $? -ne 0; then echo 'Checkout error!'; exit 1; fi
done

for it in \
 "git config user.name '${USER_NAME}'" \
 "git config user.email '${USER_EMAIL}'"; do
 docker exec -w "${WORK_DIR}" "${CONTAINER_NAME}" /usr/local/bin/bash -c "${it}"
 if test $? -ne 0; then echo "Config error!"; exit 1; fi
done

for it in \
 "git merge --no-ff --no-commit ${SOURCE_COMMIT}"; do
 docker exec -w "${WORK_DIR}" "${CONTAINER_NAME}" /usr/local/bin/bash -c "$it"
 if test $? -ne 0; then echo 'Merge error!'; exit 1; fi
done

docker cp "${VARIANT}" "${CONTAINER_NAME}:${WORK_DIR}/${VARIANT}"
if test $? -ne 0; then echo 'Copy error!'; exit 1; fi

for it in \
 "${VARIANT}/metadata/assemble.sh" \
 "${VARIANT}/vcs/commit.sh" \
 "${VARIANT}/check.sh"; do
 docker exec -w "${WORK_DIR}" "${CONTAINER_NAME}" /usr/local/bin/bash -c "$it"
 if test $? -ne 0; then echo 'Pipeline error!'; exit 1; fi
done

for it in \
 'git push && git push --tag'; do
 docker exec -w "${WORK_DIR}" "${CONTAINER_NAME}" /usr/local/bin/bash -c "$it"
 if test $? -ne 0; then echo 'Push error!'; exit 1; fi
done

docker stop "${CONTAINER_NAME}"
docker rm -f "${CONTAINER_NAME}"
