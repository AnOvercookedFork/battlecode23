# runs all maps for two bots

import os
import time

# these constants are probably fine
maps = ['AllElements', 'ArtistRendition', 'BatSignal', 'BowAndArrow', 'Cat', 'Clown', 'DefaultMap', 'Diagonal', 'Eyelands', 'Frog', 'Greivance', 'Hah', 'Jail', 'KingdomRush', 'SmallElements', 'maptestsmall', 'Minefield', 'Movepls', 'Orbit', 'Pathfind', 'Pit', 'Pizza', 'Quiet', 'Scatter', 'Sun', 'Tacocat']
maps2 = ['AllElements','ArtistRendition','BatSignal','BattleSuns','BowAndArrow','Cat','Checkmate2','Clown','Cornucopia','Crossword','Cube','DefaultMap','Diagonal','Divergence','Dreamy','Eyelands','Forest','FourNations','Frog','Grievance','Hah','HideAndSeek','HorizontallySymmetric','Jail','KingdomRush','Lantern','Lines','maptestsmall','Maze','Minefield','Movepls','Orbit','PairedProgramming','Pakbot','Pathfind','Piglets','Pit','Pizza','Quiet','Rectangle','Rewind','Risk','Scatter','Sine','SmallElements','Snowflake','SomethingFishy','Spin','Spiral','Squares','Star','Sun','Sus','SweetDreams','Tacocat','TicTacToe','Turtle','USA','VerticallySymmetric']
maps = maps2
# set these
teamA = 'torus'
teamB = 'torus1_25_5'

def get_winner(data):
    return data[-6].split(' ')[-4]

def run_matches(teamA, teamB, score):
    map_score = {}
    for map in maps:
        current = time.time()
        result = os.popen('gradlew run -Pmaps={map} -PteamA={a} -PteamB={b} -Pdebug=false -Penableprofiler=false -PoutputVerbose=false -PshowIndicators=false'.format(map = map, a = teamA, b = teamB)).read().splitlines()
        print('Last round {} took {} seconds'.format(map, time.time() - current))
        winner = get_winner(result)[1:2]
        if winner == 'A':
            score[0] += 1
            map_score[map] = teamA
        elif winner == 'B':
            score[1] += 1
            map_score[map] = teamB
        else:
            print('I don\'t know who won this, take a look: {}'.format(result))
    return score, map_score

def main():
    os.chdir('..')
    os.system('gradlew update')
    os.system('gradlew build')

    current = time.time()
    score1 = run_matches(teamA, teamB, [0, 0])
    print('Match 1 score ({} vs {}): {}'.format(teamA, teamB, score1[0]))
    print('Match 1 maps: {}'.format(score1[1]))
    score2 = run_matches(teamB, teamA, [0, 0])
    print('Match 2 score ({} vs {}): {}'.format(teamB, teamA, score2[0]))
    print('Match 2 maps: {}'.format(score2[1]))
    final_score = [score1[0][0] + score2[0][1], score1[0][1] + score2[0][0]]
    print('Final score ({} vs {}): {}'.format(teamA, teamB, final_score))

    for key in score1[1].keys():
        if score1[1][key] == score2[1][key]:
            print('{} won both sides on {}'.format(score1[1][key], key))

    print('Total runtime was {} seconds'.format(time.time() - current))

if __name__ == '__main__':
    main()