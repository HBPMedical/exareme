from __future__ import division
from __future__ import print_function

from os import path

from PCA.pca_lib import local_2
from utils.algorithm_utils import StateData, TransferAndAggregateData, parse_exareme_args


def main(args):
    fname_prev_state = path.abspath(args.prev_state_pkl)
    global_db = path.abspath(args.global_step_db)

    # Load local state
    local_state = StateData.load(fname_prev_state).get_data()
    # Load global node output
    global_out = TransferAndAggregateData.load(global_db)
    # Run algorithm local step
    local_out = local_2(args, local_state=local_state, local_in=global_out)
    # Return the output data
    local_out.transfer()


if __name__ == '__main__':
    main(parse_exareme_args(__file__))
